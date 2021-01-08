CREATE KEYSPACE IF NOT EXISTS creditcard
WITH replication = {
'class': 'SimpleStrategy', 'replication_factor': 1
};

USE creditcard;

CREATE TABLE IF NOT EXISTS fraud_transaction (
  cc_num text,
  trans_time timestamp,
  trans_num text,
  category text,
  merchant text,
  amt double,
  merch_lat double,
  merch_long double,
  distance double,
  age int,
  is_fraud double,
  PRIMARY KEY(cc_num, trans_time)
)WITH CLUSTERING ORDER BY (trans_time DESC);


CREATE TABLE IF NOT EXISTS non_fraud_transaction (
  cc_num text,
  trans_time timestamp,
  trans_num text,
  category text,
  merchant text,
  amt double,
  merch_lat double,
  merch_long double,
  distance double,
  age int,
  is_fraud double,
  PRIMARY KEY(cc_num, trans_time)
)WITH CLUSTERING ORDER BY (trans_time DESC);



CREATE TABLE IF NOT EXISTS kafka_offset (
  partition int,
  offset bigint,
  PRIMARY KEY(partition)
);


CREATE TABLE IF NOT EXISTS customer (
  cc_num text,
  first text,
  last text,
  gender text,
  street text,
  city text,
  state text,
  zip text,
  lat double,
  long double,
  job text,
  dob timestamp,
  PRIMARY KEY(cc_num)
);
