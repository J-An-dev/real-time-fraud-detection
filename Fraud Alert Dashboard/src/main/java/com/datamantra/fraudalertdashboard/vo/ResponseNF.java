package com.datamantra.fraudalertdashboard.vo;

import com.datamantra.fraudalertdashboard.dao.entity.NonFraudData;

import java.io.Serializable;
import java.util.List;


/**
 * Response object containing creditcard fraud details that will be sent to dashboard.
 */
public class ResponseNF implements Serializable {
	private List<NonFraudData> nonFraudAlert;
	
	public List<NonFraudData> getNonFraudAlert() {
		return nonFraudAlert;
	}
	public void setNonFraudAlert(List<NonFraudData> nonFraudAlert) {
		this.nonFraudAlert = nonFraudAlert;
	}

}
