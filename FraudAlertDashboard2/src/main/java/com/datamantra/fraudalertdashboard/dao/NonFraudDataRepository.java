package com.datamantra.fraudalertdashboard.dao;

import com.datamantra.fraudalertdashboard.dao.entity.NonFraudData;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * DAO class for fraud_transaction
 */
@Repository
public interface NonFraudDataRepository extends CassandraRepository<NonFraudData>{

	 @Query("SELECT cc_num, trans_time, is_fraud, trans_num, category, merchant, amt, merch_lat, merch_long, distance, age FROM creditcard.non_fraud_transaction WHERE trans_time > ?0 ALLOW FILTERING" )
	 Iterable<NonFraudData> findFraudDataByTimestamp(Long timestamp);
}
