package com.datamantra.spark

import java.net.InetAddress

import com.datamantra.cassandra.CassandraConfig
import com.datamantra.config.Config
import com.datamantra.kafka.KafkaConfig
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.streaming.Duration

/**
  * Created by kafka on 9/5/18.
  */
object SparkConfig {
   val logger = Logger.getLogger(getClass.getName)

   val sparkConf = new SparkConf

   var transactionDatasouce:String = _
   var customerDatasource:String = _
   var modelPath:String = _
   var preprocessingModelPath:String = _
   var shutdownMarker:String = _
   var batchInterval:Int = _


    def load() = {
      logger.info("Loading Spark Setttings")
      sparkConf.set("spark.streaming.stopGracefullyOnShutdown", Config.applicationConf.getString("config.spark.gracefulShutdown"))
        .set("spark.sql.streaming.checkpointLocation", Config.applicationConf.getString("config.spark.checkpoint"))
        .set("spark.cassandra.connection.host", Config.applicationConf.getString("config.cassandra.host"))
      shutdownMarker = Config.applicationConf.getString("config.spark.shutdownPath")
      batchInterval = Config.applicationConf.getString("config.spark.batch.interval").toInt
      transactionDatasouce = Config.localProjectDir + Config.applicationConf.getString("config.spark.transaction.datasource")
      customerDatasource = Config.localProjectDir + Config.applicationConf.getString("config.spark.customer.datasource")
      modelPath = Config.localProjectDir + Config.applicationConf.getString("config.spark.model.path")
      preprocessingModelPath = Config.localProjectDir + Config.applicationConf.getString("config.spark.model.preprocessing.path")
    }

    def defaultSetting() = {
      sparkConf.setMaster("local[*]")
        .set("spark.cassandra.connection.host", CassandraConfig.cassandrHost)
        .set("spark.sql.streaming.checkpointLocation", "/tmp/checkpoint")
      shutdownMarker = "/tmp/shutdownmarker"
      transactionDatasouce = "src/main/resources/data/transactions.csv"
      customerDatasource = "src/main/resources/data/customer.csv"
      modelPath = "src/main/resources/spark/training/RandomForestModel"
      preprocessingModelPath = "src/main/resources/spark/training/PreprocessingModel"
      batchInterval = 5000

    }
 }
