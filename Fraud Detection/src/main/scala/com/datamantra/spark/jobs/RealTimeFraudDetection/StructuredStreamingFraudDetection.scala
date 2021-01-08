package com.datamantra.spark.jobs.RealTimeFraudDetection

import com.datamantra.cassandra.{CassandraConfig, CassandraDriver}
import com.datamantra.config.Config
import com.datamantra.kafka.KafkaSource
import com.datamantra.spark.jobs.SparkJob
import com.datamantra.spark.{GracefulShutdown, DataReader, SparkConfig}
import com.datamantra.utils.Utils
import org.apache.log4j.Logger
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Created by kafka on 14/5/18.
 */
object StructuredStreamingFraudDetection extends SparkJob("Structured Streaming Job to detect fraud transaction"){

  val logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]) {

    Config.parseArgs(args)
    import sparkSession.implicits._

    val customerDF = DataReader.readFromCassandra(CassandraConfig.keyspace, CassandraConfig.customer)
    val customerAgeDF = customerDF.withColumn("age", (datediff(current_date(),to_date($"dob"))/365).cast(IntegerType))
    customerAgeDF.cache()


    /*Offset is read from checkpointing, hence reading offset and saving offset to /from Cassandra is not required*/
    //val (startingOption, partitionsAndOffsets) = CassandraDriver.readOffset(CassandraConfig.keyspace, CassandraConfig.kafkaOffsetTable)

    val rawStream = KafkaSource.readStream()//(startingOption, partitionsAndOffsets)

    val transactionStream = rawStream
      .selectExpr("transaction.*", "partition", "offset")
      .withColumn("amt", lit($"amt") cast(DoubleType))
      .withColumn("merch_lat", lit($"merch_lat") cast(DoubleType))
      .withColumn("merch_long", lit($"merch_long") cast(DoubleType))
      .drop("first")
      .drop("last")

    val distanceUdf = udf(Utils.getDistance _)

    sparkSession.sqlContext.sql("SET spark.sql.autoBroadcastJoinThreshold = 52428800")
    val processedTransactionDF = transactionStream.join(broadcast(customerAgeDF), Seq("cc_num"))
      .withColumn("distance", lit(round(distanceUdf($"lat", $"long", $"merch_lat", $"merch_long"), 2)))
      .select($"cc_num", $"trans_num", to_timestamp($"trans_time", "yyyy-MM-dd HH:mm:ss") as "trans_time", $"category", $"merchant", $"amt", $"merch_lat", $"merch_long", $"distance", $"age", $"partition", $"offset")


        val coloumnNames = List("cc_num", "category", "merchant", "distance", "amt", "age")

        val preprocessingModel = PipelineModel.load(SparkConfig.preprocessingModelPath)
        val featureTransactionDF = preprocessingModel.transform(processedTransactionDF)

        val randomForestModel = RandomForestClassificationModel.load(SparkConfig.modelPath)
        val predictionDF =  randomForestModel.transform(featureTransactionDF).withColumnRenamed("prediction", "is_fraud")
        //predictionDF.cache

        val fraudPredictionDF = predictionDF.filter($"is_fraud" === 1.0)

        val nonFraudPredictionDF = predictionDF.filter($"is_fraud" =!= 1.0)

        /*Save fraud transactions to fraud_transaction table*/
        val fraudQuery = CassandraDriver.saveForeach(fraudPredictionDF, CassandraConfig.keyspace, CassandraConfig.fraudTransactionTable, "fraudQuery", "append")

        /*Save non fraud transactions to non_fraud_transaction table*/
        val nonFraudQuery = CassandraDriver.saveForeach(nonFraudPredictionDF, CassandraConfig.keyspace, CassandraConfig.nonFraudTransactionTable, "nonFraudQuery", "append")

        /*Offset is read from checkpointing, hence reading offset and saving offset to /from Cassandra is not required*/
        /*val kafkaOffsetDF = predictionDF.select("partition", "offset").groupBy("partition").agg(max("offset") as "offset")
        val offsetQuery = CassandraDriver.saveForeach(kafkaOffsetDF, CassandraConfig.keyspace, CassandraConfig.kafkaOffsetTable, "offsetQuery", "update")*/

        GracefulShutdown.handleGracefulShutdown(1000, List(fraudQuery, nonFraudQuery))

  }
}
