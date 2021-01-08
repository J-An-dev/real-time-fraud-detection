package com.datamantra.spark.jobs.RealTimeFraudDetection

import com.datamantra.cassandra.dao.{KafkaOffsetRepository, CreditcardTransactionRepository}
import com.datamantra.cassandra.{CassandraDriver, CassandraConfig}
import com.datamantra.config.Config
import com.datamantra.creditcard.Schema
import com.datamantra.kafka.KafkaConfig
import com.datamantra.spark.{DataReader, GracefulShutdown, SparkConfig}
import com.datamantra.spark.jobs.SparkJob
import com.datamantra.utils.Utils
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.log4j.Logger
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{TimestampType, DoubleType, IntegerType}
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies._


import scala.collection.mutable.Map


object DstreamFraudDetection extends SparkJob("Fraud Detection using Dstream"){

  val logger = Logger.getLogger(getClass.getName)

  def main (args: Array[String]){

    Config.parseArgs(args)

    import sparkSession.implicits._
    val customerDF = DataReader.readFromCassandra(CassandraConfig.keyspace, CassandraConfig.customer)
    val customerAgeDF = customerDF.withColumn("age", (datediff(current_date(),to_date($"dob"))/365).cast(IntegerType))
    customerAgeDF.cache()

    /* Load Preprocessing Model and Random Forest Model saved by Spark ML Job i.e FraudDetectionTraining */
    val preprocessingModel = PipelineModel.load(SparkConfig.preprocessingModelPath)
    val randomForestModel = RandomForestClassificationModel.load(SparkConfig.modelPath)

    /*
       Connector Object is created in driver. It is serializable.
       So once the executor get it, they establish the real connection
    */
    val connector = CassandraConnector(sparkSession.sparkContext.getConf)

    val brodcastMap = sparkSession.sparkContext.broadcast(Map("keyspace" -> CassandraConfig.keyspace,
      "fraudTable" -> CassandraConfig.fraudTransactionTable,
      "nonFraudTable" -> CassandraConfig.nonFraudTransactionTable,
      "kafkaOffsetTable" -> CassandraConfig.kafkaOffsetTable))


    val ssc = new StreamingContext(sparkSession.sparkContext, Duration(SparkConfig.batchInterval))


    val topics = Set(KafkaConfig.kafkaParams("topic"))
    val kafkaParams = Map[String, String](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> KafkaConfig.kafkaParams("bootstrap.servers"),
      ConsumerConfig.GROUP_ID_CONFIG -> KafkaConfig.kafkaParams("group.id"),
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG ->
        "org.apache.kafka.common.serialization.StringDeserializer",
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->
        "org.apache.kafka.common.serialization.StringDeserializer",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> KafkaConfig.kafkaParams("auto.offset.reset")
    )


    val storedOffsets = CassandraDriver.readOffset(CassandraConfig.keyspace,
                           CassandraConfig.kafkaOffsetTable, KafkaConfig.kafkaParams("topic"))

    val stream = storedOffsets match {
      case None => {
        KafkaUtils.createDirectStream[String, String](ssc,
                       PreferConsistent,
                       Subscribe[String, String](topics, kafkaParams)
                       )
      }

      case Some(fromOffsets) => {
        KafkaUtils.createDirectStream[String, String](ssc,
                       PreferConsistent,
                       Assign[String, String](fromOffsets.keys.toList, kafkaParams, fromOffsets))
      }
    }

    val transactionStream =  stream.map(cr => (cr.value(), cr.partition(), cr.offset()))

    transactionStream.foreachRDD(rdd => {

      if (!rdd.isEmpty()) {

        val kafkaTransactionDF = rdd.toDF("transaction", "partition", "offset")
          .withColumn(Schema.kafkaTransactionStructureName, // nested structure with our json
            from_json($"transaction", Schema.kafkaTransactionSchema)) //From binary to JSON object
          .select("transaction.*", "partition", "offset")
          .withColumn("amt", lit($"amt") cast (DoubleType))
          .withColumn("merch_lat", lit($"merch_lat") cast (DoubleType))
          .withColumn("merch_long", lit($"merch_long") cast (DoubleType))
          .withColumn("trans_time", lit($"trans_time") cast (TimestampType))


        val distanceUdf = udf(Utils.getDistance _)

        sparkSession.sqlContext.sql("SET spark.sql.autoBroadcastJoinThreshold = 52428800")
        val processedTransactionDF = kafkaTransactionDF.join(broadcast(customerAgeDF), Seq("cc_num"))
          .withColumn("distance", lit(round(distanceUdf($"lat", $"long", $"merch_lat", $"merch_long"), 2)))


        val featureTransactionDF = preprocessingModel.transform(processedTransactionDF)
        val predictionDF = randomForestModel.transform(featureTransactionDF)
          .withColumnRenamed("prediction", "is_fraud")

        /*
         Connector Object is created in driver. It is serializable.
         It is serialized and send to executor. Once the executor get it, they establish the real connection
        */

        predictionDF.foreachPartition(partitionOfRecords => {

          /*
          * dbname and table name are initialzed in the driver. foreachPartition is called in the executor, hence dbname
          * and table names have to be broadcasted
          */
          val keyspace = brodcastMap.value("keyspace")
          val fraudTable = brodcastMap.value("fraudTable")
          val nonFraudTable = brodcastMap.value("nonFraudTable")
          val kafkaOffsetTable = brodcastMap.value("kafkaOffsetTable")

 /*
          Writing to Fraud, NonFraud and Offset Table in single iteration
          Cassandra prepare statement is used because it avoids parsing of the column for every insert and hence efficient
          Offset is inserted last to achieve at least once semantics. it is possible that it may read duplicate credtcard
          transactions from kafka while restart.
          Even though duplicate creditcard transaction are read from kafka, writing to Cassandra is idempotent. Because
          cc_num and trans_time is the primary key. So you cannot have duplicate records with same cc_num and trans_time.
          As a result we achieve exactly once semantics.
*/

          connector.withSessionDo(session => {
            //Prepare Statement for all three tables
            val preparedStatementFraud = session.prepare(CreditcardTransactionRepository.cqlTransactionPrepare(keyspace, fraudTable))
            val preparedStatementNonFraud = session.prepare(CreditcardTransactionRepository.cqlTransactionPrepare(keyspace, nonFraudTable))
            val preparedStatementOffset = session.prepare(KafkaOffsetRepository.cqlOffsetPrepare(keyspace, kafkaOffsetTable))

            val partitionOffset:Map[Int, Long] = Map.empty
            partitionOfRecords.foreach(record => {
              val isFraud = record.getAs[Double]("is_fraud")
              if (isFraud == 1.0) {
                // Bind and execute prepared statement for Fraud Table
                session.execute(CreditcardTransactionRepository.cqlTransactionBind(preparedStatementFraud, record))
              }
              else if(isFraud == 0.0) {
                // Bind and execute prepared statement for NonFraud Table
                session.execute(CreditcardTransactionRepository.cqlTransactionBind(preparedStatementNonFraud, record))
              }
              //Get max offset in the current match
              val kafkaPartition = record.getAs[Int]("partition")
              val offset = record.getAs[Long]("offset")
              partitionOffset.get(kafkaPartition) match  {
                case None => partitionOffset.put(kafkaPartition, offset)
                case Some(currentMaxOffset) => {
                  if(offset > currentMaxOffset)
                    partitionOffset.update(kafkaPartition, offset)
                }

              }

            })
            partitionOffset.foreach(t => {
              // Bind and execute prepared statement for Offset Table
              session.execute(KafkaOffsetRepository.cqlOffsetBind(preparedStatementOffset, t))

            })

          })
        })


      }
      else {
        logger.info("Did not receive any data")
      }

    })

    ssc.start()
    GracefulShutdown.handleGracefulShutdown(1000, ssc)
  }

}
