package com.datamantra.cassandra

import com.datamantra.cassandra.foreachSink.CassandraSinkForeach
import com.datamantra.spark.SparkConfig
import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.kafka.common.TopicPartition
import org.apache.log4j.Logger

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._

import org.apache.spark.sql.functions._
import org.apache.spark.streaming.kafka010.HasOffsetRanges


/**
 * Created by kafka on 16/5/18.
 */
object CassandraDriver {

  val logger = Logger.getLogger(getClass.getName)

  val connector = CassandraConnector(SparkConfig.sparkConf)


  def debugStream(ds: Dataset[_], mode: String = "append") = {

    ds.writeStream
      .format("console")
      .option("truncate", "false")
      .option("numRows", "100")
      .outputMode(mode)
      .start()
  }


  def saveForeach(df: DataFrame, db:String, table:String , queryName: String, mode:String) = {

    println("Calling saveForeach")
    df
      .writeStream
      .queryName(queryName)
      .outputMode(mode)
      .foreach(new CassandraSinkForeach(db, table))
      .start()
  }


  /* Read and prepare offset for Structrued Streaming */
  def readOffset(keyspace:String, table:String)(implicit sparkSession:SparkSession) = {

    import sparkSession.implicits._
    val df = sparkSession
      .read
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace",keyspace)
      .option("table",table)
      .option("pushdown", "true")
      .load()
      .select("partition", "offset")
      //.filter($"partition".isNotNull)
    //df.show(false)

    if( df.rdd.isEmpty()) {
      ("startingOffsets", "earliest")
    }
    else {
      /*
      val offsetDf = df.select("partition", "offset")
        .groupBy("partition").agg(max("offset") as "offset")
      ("startingOffsets", transformKafkaMetadataArrayToJson(offsetDf.collect()))
      */
      ("startingOffsets", transformKafkaMetadataArrayToJson(df.collect()))
    }
  }

  /**
   * @param array
   * @return {"topicA":{"0":23,"1":-1},"topicB":{"0":-2}}
   */
  def transformKafkaMetadataArrayToJson(array: Array[Row]) = {

    val partitionOffset = array
      .toList
      .foldLeft("")((a, i) => {
        a + s""""${i.getAs[Int](("partition"))}":${i.getAs[Long](("offset"))}, """
      })

    println("Offset: " + partitionOffset.substring(0, partitionOffset.size -2))

    val partitionAndOffset  = s"""{"creditTransaction":
          {
           ${partitionOffset.substring(0, partitionOffset.size -2)}
          }
         }
      """.replaceAll("\n", "").replaceAll(" ", "")
    println(partitionAndOffset)
    partitionAndOffset
  }



  /* Read offsert from Cassandra for Dstream*/
  def readOffset(keySpace:String, table:String, topic:String)(implicit sparkSession:SparkSession) = {

    import sparkSession.implicits._
    val df = sparkSession
      .read
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace",keySpace)
      .option("table",table)
      .option("pushdown", "true")
      .load()
      .select("partition", "offset")

    if (df.rdd.isEmpty()) {
      logger.info("No offset. Read from earliest")
      None
    }
    else {
      val fromOffsets = df.rdd.collect().map(o => {
        println(o)
        (new TopicPartition(topic, o.getAs[Int]("partition")), o.getAs[Long]("offset"))
      }
      ).toMap
      Some(fromOffsets)
    }
  }


  /* Save Offset to Cassandra for Structured Streaming */
  def saveOffset(keySpace:String, table:String, df:DataFrame)(implicit sparkSession:SparkSession) = {

    import sparkSession.implicits._

    df.write
     .format("org.apache.spark.sql.cassandra")
     .options(Map("keyspace" -> keySpace, "table" -> table))
     .save()
  }

 }
