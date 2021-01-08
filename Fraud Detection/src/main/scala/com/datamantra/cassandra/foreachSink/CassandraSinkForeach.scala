package com.datamantra.cassandra.foreachSink


import java.sql.Timestamp
import java.util.Date

import com.datamantra.cassandra.{CassandraConfig, CassandraDriver}
import com.datamantra.creditcard.Enums
import org.apache.log4j.Logger
import org.apache.spark.sql.{Row, ForeachWriter}

/**
 * Created by kafka on 16/5/18.
 */


class CassandraSinkForeach(dbName:String, tableName:String) extends ForeachWriter[Row] {

  //val logger = Logger.getLogger(getClass.getName)

  val db = dbName
  val table = tableName

  private def cqlTransaction(record: Row): String = s"""
     insert into $db.$table (
       ${Enums.TransactionCassandra.cc_num},
       ${Enums.TransactionCassandra.trans_time},
       ${Enums.TransactionCassandra.trans_num},
       ${Enums.TransactionCassandra.category},
       ${Enums.TransactionCassandra.merchant},
       ${Enums.TransactionCassandra.amt},
       ${Enums.TransactionCassandra.merch_lat},
       ${Enums.TransactionCassandra.merch_long},
       ${Enums.TransactionCassandra.distance},
       ${Enums.TransactionCassandra.age},
       ${Enums.TransactionCassandra.is_fraud}
     )
     values(
       '${record.getAs[String](Enums.TransactionCassandra.cc_num)  }',
       '${record.getAs[Timestamp](Enums.TransactionCassandra.trans_time)}',
       '${record.getAs[String](Enums.TransactionCassandra.trans_num)}',
       '${record.getAs[String](Enums.TransactionCassandra.category)}',
       '${record.getAs[String](Enums.TransactionCassandra.merchant)}',
        ${record.getAs[Double](Enums.TransactionCassandra.amt)},
        ${record.getAs[Double](Enums.TransactionCassandra.merch_lat)},
        ${record.getAs[Double](Enums.TransactionCassandra.merch_long)},
        ${record.getAs[Double](Enums.TransactionCassandra.distance)},
        ${record.getAs[Double](Enums.TransactionCassandra.age)},
        ${record.getAs[Double](Enums.TransactionCassandra.is_fraud)}
        )"""


  private def cqlOffset(record: Row): String = s"""
     insert into $db.$table (
       ${Enums.TransactionCassandra.kafka_partition},
       ${Enums.TransactionCassandra.kafka_offset}
     )
     values(
        ${record.getAs[Int](Enums.TransactionCassandra.kafka_partition)},
        ${record.getAs[Long](Enums.TransactionCassandra.kafka_offset)}
        )"""

  def open(partitionId: Long, version: Long): Boolean = {
    // open connection
    //@TODO command to check if cassandra cluster is up
    true
  }

  //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/1_connecting.md#connection-pooling
  def process(record: Row) = {
    if (table == CassandraConfig.fraudTransactionTable || table == CassandraConfig.nonFraudTransactionTable) {
      println(s"Saving record: $record")
      CassandraDriver.connector.withSessionDo(session =>
        session.execute(cqlTransaction(record))
      )
    }
    else if(table == CassandraConfig.kafkaOffsetTable) {
      println(s"Saving offset to kafka: $record")
      CassandraDriver.connector.withSessionDo(session => {
        session.execute(cqlOffset(record))
      })
    }
  }

  //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md#cassandra-connection-parameters

  def close(errorOrNull: Throwable): Unit = {

    //CassandraDriver.connector.withClusterDo(session => session.close())
    // close the connection
    //connection.keep_alive_ms	--> 5000ms :	Period of time to keep unused connections open
  }
}
