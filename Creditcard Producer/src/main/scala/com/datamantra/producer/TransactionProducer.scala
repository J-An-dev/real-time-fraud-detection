package com.datamantra.producer


import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util._

import com.google.gson.{JsonObject, Gson}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import java.nio.charset.Charset;
import org.apache.kafka.clients.producer._


object TransactionProducer {

  var applicationConf:Config = _
  val props = new Properties()
  var topic:String =  _
  var producer:KafkaProducer[String, String] = _

  def load = {

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, applicationConf.getString("kafka.bootstrap.servers"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, applicationConf.getString("kafka.key.serializer"))
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, applicationConf.getString("kafka.value.serializer"))
    props.put(ProducerConfig.ACKS_CONFIG, applicationConf.getString("kafka.acks"))
    props.put(ProducerConfig.RETRIES_CONFIG, applicationConf.getString("kafka.retries"))
    topic = applicationConf.getString("kafka.topic")
  }

  def getCsvIterator(fileName:String) = {

    val file = new File(fileName)
    val csvParser = CSVParser.parse(file, Charset.forName("UTF-8"), CSVFormat.DEFAULT)
    csvParser.iterator()
  }

/*
  def publishAvroRecord(fileName:String ): Unit = {

    val csvIterator = getCsvIterator(fileName)
    val rand: Random = new Random
    var count = 0
    while (csvIterator.hasNext) {

      val record = csvIterator.next()
      val transaction = new Transaction()
      println("Transaction Details:" + record.get(0),record.get(1),record.get(2),record.get(3),record.get(4),record.get(5),record.get(6),record.get(7),record.get(8),record.get(9), record.get(10), record.get(11))
      transaction.setCcNum(record.get(0))
      transaction.setFirst(record.get(1))
      transaction.setLast(record.get(2))
      transaction.setTransNum(record.get(3))
      transaction.setTransDate(record.get(4))
      transaction.setTransTime(record.get(5))
      transaction.setUnixTime(record.get(6))
      transaction.setCategory(record.get(7))
      transaction.setMerchant(record.get(8))
      transaction.setAmt(record.get(9).toDouble)
      transaction.setMerchLat(record.get(10))
      transaction.setMerchLong(record.get(11))
      val producerRecord = new ProducerRecord[String, Transaction](topic, transaction)
      Thread.sleep(rand.nextInt(3000 - 1000) + 1000)
      count = count + 1
    }
    println("record count: " + count)
  }
*/

  def publishJsonMsg(fileName:String) = {
    val gson: Gson = new Gson
    val csvIterator = getCsvIterator(fileName)
    val rand: Random = new Random
    var count = 0

    while (csvIterator.hasNext) {
      val record = csvIterator.next()

      val obj: JsonObject = new JsonObject
      val isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      isoFormat.setTimeZone(TimeZone.getTimeZone("IST"));
      val d = new Date()
      val timestamp = isoFormat.format(d)
      val unix_time = d.getTime


      //println("Transaction Details:" + record.get(0),record.get(1),record.get(2),record.get(3),timestamp, record.get(7),record.get(8),record.get(9), record.get(10), record.get(11))

      obj.addProperty(TransactionKafkaEnum.cc_num, record.get(0))
      obj.addProperty(TransactionKafkaEnum.first, record.get(1))
      obj.addProperty(TransactionKafkaEnum.last, record.get(2))
      obj.addProperty(TransactionKafkaEnum.trans_num, record.get(3))
      obj.addProperty(TransactionKafkaEnum.trans_time, timestamp)
      //obj.addProperty(TransactionKafkaEnum.unix_time, unix_time)
      obj.addProperty(TransactionKafkaEnum.category, record.get(7))
      obj.addProperty(TransactionKafkaEnum.merchant, record.get(8))
      obj.addProperty(TransactionKafkaEnum.amt, record.get(9))
      obj.addProperty(TransactionKafkaEnum.merch_lat, record.get(10))
      obj.addProperty(TransactionKafkaEnum.merch_long, record.get(11))
      val json: String = gson.toJson(obj)
      println("Transaction: " + json)
      val producerRecord = new ProducerRecord[String, String](topic, json) //Round Robin Partitioner
      //val producerRecord = new ProducerRecord[String, String](topic, json.hashCode.toString, json)  //Hash Partitioner
      //val producerRecord = new ProducerRecord[String, String](topic, 1, json.hashCode.toString, json)  //Specific Partition
      //producer.send(producerRecord) //Fire and Forget
      //producer.send(producerRecord).get() /* Synchronous Producer */
      producer.send(producerRecord, new MyProducerCallback) /* Asynchronous Producer */
      Thread.sleep(rand.nextInt(3000 - 1000) + 1000)
    }
  }

  class MyProducerCallback extends Callback {
    def onCompletion(recordMetadata: RecordMetadata, e: Exception) {
      if (e != null) System.out.println("AsynchronousProducer failed with an exception" + e)
      else {
        System.out.println("Partition: " + recordMetadata.partition + " and Offset: " + recordMetadata.offset + "\n")
      }
    }
  }

  def main(args: Array[String]) {

    applicationConf = ConfigFactory.parseFile(new File(args(0)))
    load
    producer = new KafkaProducer[String, String](props)
    val file = applicationConf.getString("kafka.producer.file")
    publishJsonMsg(file)


  }
}
