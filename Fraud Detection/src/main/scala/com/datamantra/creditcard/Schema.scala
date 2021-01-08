package com.datamantra.creditcard


import org.apache.spark.sql.types._

/**
 * Created by kafka on 25/5/18.
 */
object Schema {



  val transactionStructureName = "transaction"

  val transactionSchema = new StructType()
    .add(Enums.TransactionKafka.cc_num, StringType,true)
    .add(Enums.TransactionKafka.first, StringType, true)
    .add(Enums.TransactionKafka.last, StringType, true)
    .add(Enums.TransactionKafka.trans_num, StringType, true)
    .add(Enums.TransactionKafka.trans_date, StringType, true)
    .add(Enums.TransactionKafka.trans_time, StringType, true)
    .add(Enums.TransactionKafka.unix_time, LongType, true)
    .add(Enums.TransactionKafka.category, StringType, true)
    .add(Enums.TransactionKafka.merchant, StringType, true)
    .add(Enums.TransactionKafka.amt, DoubleType, true)
    .add(Enums.TransactionKafka.merch_lat, DoubleType, true)
    .add(Enums.TransactionKafka.merch_long, DoubleType, true)

  /* Transaction  Schema used while importing transaction data to Cassandra*/
  val fruadCheckedTransactionSchema = transactionSchema.add(Enums.TransactionKafka.is_fraud, DoubleType, true)

  /* Customer Schema used while importing customer data to Cassandra*/
  val customerStructureName = "customer"
  val customerSchema = new StructType()
    .add(Enums.Customer.cc_num, StringType, true)
    .add(Enums.Customer.first, StringType, true)
    .add(Enums.Customer.last, StringType, true)
    .add(Enums.Customer.gender, StringType, true)
    .add(Enums.Customer.street, StringType, true)
    .add(Enums.Customer.city, StringType, true)
    .add(Enums.Customer.state, StringType, true)
    .add(Enums.Customer.zip, StringType, true)
    .add(Enums.Customer.lat, DoubleType, true)
    .add(Enums.Customer.long, DoubleType, true)
    .add(Enums.Customer.job, StringType, true)
    .add(Enums.Customer.dob, TimestampType, true)


  /* Schema of transaction msgs received from Kafka. Json msg is received from Kafka. Hence evey field is treated as String */
  val kafkaTransactionStructureName = transactionStructureName
  val kafkaTransactionSchema = new StructType()
    .add(Enums.TransactionKafka.cc_num, StringType,true)
    .add(Enums.TransactionKafka.first, StringType, true)
    .add(Enums.TransactionKafka.last, StringType, true)
    .add(Enums.TransactionKafka.trans_num, StringType, true)
    .add(Enums.TransactionKafka.trans_time, TimestampType, true)
    .add(Enums.TransactionKafka.category, StringType, true)
    .add(Enums.TransactionKafka.merchant, StringType, true)
    .add(Enums.TransactionKafka.amt, StringType, true)
    .add(Enums.TransactionKafka.merch_lat, StringType, true)
    .add(Enums.TransactionKafka.merch_long, StringType, true)

}
