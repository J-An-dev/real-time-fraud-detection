package com.datamantra.fraudalertdashboard.vo;

import com.datamantra.fraudalertdashboard.dao.entity.FraudAlertData;

import java.io.Serializable;
import java.util.List;



/**
 * Response object containing creditcard fraud details that will be sent to dashboard.
 * 
 * @author kafka
 *
 */
public class Response implements Serializable {
	private List<FraudAlertData> fraudAlert;
	
	public List<FraudAlertData> getFraudAlert() {
		return fraudAlert;
	}
	public void setFraudAlert(List<FraudAlertData> fraudAlert) {
		this.fraudAlert = fraudAlert;
	}

}
