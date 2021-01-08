package com.datamantra.fraudalertdashboard.dao;

import com.datamantra.fraudalertdashboard.dao.entity.FraudAlertData;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * DAO class for fraud_transaction
 */
@Repository
public interface FraudAlertDataRepository extends CassandraRepository<FraudAlertData>{

	 @Query("SELECT cc_num, trans_time, is_fraud, trans_num, category, merchant, amt, merch_lat, merch_long, distance, age FROM creditcard.fraud_transaction WHERE trans_time > ?0 ALLOW FILTERING" )
	 Iterable<FraudAlertData> findFraudDataByTimestamp(Long timestamp);
}
