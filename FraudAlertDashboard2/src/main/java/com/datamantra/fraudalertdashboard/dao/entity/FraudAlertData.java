package com.datamantra.fraudalertdashboard.dao.entity;

import java.io.Serializable;
import java.util.Date;

import org.springframework.cassandra.core.Ordering;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.Column;
import org.springframework.data.cassandra.mapping.Indexed;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Entity class for fraud_transaction db table
 * 
 * @author kafka
 *
 */
@Table("fraud_transaction")
public class FraudAlertData implements Serializable{

	@PrimaryKeyColumn(name = "cc_num",ordinal = 0,type = PrimaryKeyType.PARTITIONED)
	private String cc_num;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "IST")
	@PrimaryKeyColumn(name = "trans_time",ordinal = 1,type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
	private Date trans_time;
	@Indexed(value = "is_fraud")
	private Double is_fraud;
	@Column(value = "trans_num")
	private String trans_num;
	@Column(value = "category")
	private String category;
	@Column(value = "merchant")
	private String merchant;
	@Column(value = "amt")
	private Double amt;
	@Column(value = "merch_lat")
	private Double merch_lat;
	@Column(value = "merch_long")
	private Double merch_long;
	@Column(value = "distance")
	private Double distance;
	@Column(value = "age")
	private Integer age;

	public String getCc_num() {
		return cc_num;
	}

	public void setCc_num(String cc_num) {
		this.cc_num = cc_num;
	}

	public Date getTrans_time() {
		return trans_time;
	}

	public void setTrans_time(Date trans_time) {
		this.trans_time = trans_time;
	}

	public Double getIs_fraud() {
		return is_fraud;
	}

	public void setIs_fraud(Double is_fraud) {
		this.is_fraud = is_fraud;
	}

	public String getTrans_num() {
		return trans_num;
	}

	public void setTrans_num(String trans_num) {
		this.trans_num = trans_num;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getMerchant() {
		return merchant;
	}

	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	public Double getAmt() {
		return amt;
	}

	public void setAmt(Double amt) {
		this.amt = amt;
	}

	public Double getMerch_lat() {
		return merch_lat;
	}

	public void setMerch_lat(Double merch_lat) {
		this.merch_lat = merch_lat;
	}

	public Double getMerch_long() {
		return merch_long;
	}

	public void setMerch_long(Double merch_long) {
		this.merch_long = merch_long;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "FraudAlertData{" +
				"cc_num='" + cc_num + '\'' +
				", trans_time=" + trans_time +
				", is_fraud=" + is_fraud +
				", trans_num='" + trans_num + '\'' +
				", category='" + category + '\'' +
				", merchant='" + merchant + '\'' +
				", amt=" + amt +
				", merch_lat=" + merch_lat +
				", merch_long=" + merch_long +
				", distance=" + distance +
				", age=" + age +
				'}';
	}

}
