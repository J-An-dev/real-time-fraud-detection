package com.datamantra.producer

/**
 * Created by kafka on 24/5/18.
 */
object TransactionKafkaEnum extends  Enumeration{

  val cc_num = "cc_num"
  val first = "first"
  val last = "last"
  val trans_num = "trans_num"
  val trans_date = "trans_date"
  val trans_time = "trans_time"
  val unix_time = "unix_time"
  val category = "category"
  val merchant = "merchant"
  val amt = "amt"
  val merch_lat = "merch_lat"
  val merch_long = "merch_long"
  val distance = "distance"
  val age = "age"
  val is_fraud = "is_fraud"
  val kafka_partition = "partition"
  val kafka_offset = "offset"

}
