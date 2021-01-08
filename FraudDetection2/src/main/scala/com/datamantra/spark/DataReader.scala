package com.datamantra.spark

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import org.apache.spark.streaming.kafka010.HasOffsetRanges

/**
 * Created by kafka on 15/6/18.
 */
object DataReader {

  val logger = Logger.getLogger(getClass.getName)

  def read(transactionDatasource:String, schema:StructType)(implicit sparkSession:SparkSession) = {
    sparkSession.read
      .option("header", "true")
      .schema(schema)
      .csv(transactionDatasource)
  }

  def readFromCassandra(keySpace:String, table:String)(implicit sparkSession:SparkSession) = {

    sparkSession.read
      .format("org.apache.spark.sql.cassandra")
      .options(Map("keyspace" -> keySpace, "table" -> table, "pushdown" -> "true"))
      .load()
  }

  def getOffset(rdd: RDD[_])(implicit sparkSession:SparkSession) = {

    import  sparkSession.implicits._
    rdd.asInstanceOf[HasOffsetRanges]
      .offsetRanges.toList
      .map(offset => (offset.partition, offset.untilOffset))
      .toDF("partition", "offset")
  }
}
