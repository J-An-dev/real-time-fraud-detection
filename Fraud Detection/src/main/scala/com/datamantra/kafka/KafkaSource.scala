package com.datamantra.kafka

import com.datamantra.config.Config
import com.datamantra.creditcard.{Schema, TransactionKafka}
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.log4j.Logger
import org.apache.spark.sql.{SparkSession, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import com.typesafe.config.ConfigFactory

import scala.collection.mutable.Map


/**
 * Created by kafka on 16/5/18.
 */
object KafkaSource {

  val logger = Logger.getLogger(getClass.getName)

  /* Read stream from Kafka using Structured Streaming */
  def readStream(startingOption: String = "startingOffsets", partitionsAndOffsets: String = "earliest")(implicit sparkSession:SparkSession) = {
    logger.info("Reading from Kafka")
    //logger.info("partitionsAndOffsets: " + partitionsAndOffsets)
    import  sparkSession.implicits._
    sparkSession
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", KafkaConfig.kafkaParams("bootstrap.servers"))
      .option("subscribe", KafkaConfig.kafkaParams("topic"))
      .option("enable.auto.commit", KafkaConfig.kafkaParams("enable.auto.commit").toBoolean) // Cannot be set to true in Spark Strucutured Streaming https://spark.apache.org/docs/latest/structured-streaming-kafka-integration.html#kafka-specific-configurations
      .option("group.id", KafkaConfig.kafkaParams("group.id"))
      //.option(startingOption, partitionsAndOffsets) //this only applies when a new query is started and that resuming will always pick up from where the query left off
      .load()
      .withColumn(Schema.kafkaTransactionStructureName, // nested structure with our json
       from_json($"value".cast(StringType), Schema.kafkaTransactionSchema)) //From binary to JSON object
      .as[TransactionKafka]
  }

}
