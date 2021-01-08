package com.datamantra.fraudalertdashboard.dashboard;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


import com.datamantra.fraudalertdashboard.dao.FraudAlertDataRepository;
import com.datamantra.fraudalertdashboard.dao.entity.FraudAlertData;
import com.datamantra.fraudalertdashboard.vo.Response;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


/*
 * Service class to send fraud transaction messages to dashboard ui at fixed interval using web-socket.
 */
@Service
public class FraudAlertService {
	private static final Logger logger = Logger.getLogger(FraudAlertService.class);


	@Autowired
	private SimpMessagingTemplate template;
	
	@Autowired
	private FraudAlertDataRepository fraudAlertRepository;
	
	private long previous_timestamp=0L;
	
	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	//Method sends fraud transaction message in every 5 seconds.
	@Scheduled(fixedRate = 5000)
	public void trigger() {

		List<FraudAlertData> recentFraudList = new ArrayList<FraudAlertData>();
		//Call dao methods
		if(previous_timestamp==0) {
			fraudAlertRepository.findFraudDataByTimestamp(new Date().getTime() - 10000).forEach(e -> {
				recentFraudList.add(e);
			});
			Collections.sort(recentFraudList, new CustomComparator());
			previous_timestamp = recentFraudList.get(0).getTrans_time().getTime();
		}
		else {
			fraudAlertRepository.findFraudDataByTimestamp(previous_timestamp).forEach(e -> {
				recentFraudList.add(e);
			});
			Collections.sort(recentFraudList, new CustomComparator());
			previous_timestamp = recentFraudList.get(0).getTrans_time().getTime();
		}
		logger.info("previous_timestamp: " + previous_timestamp);


		//prepare response
		Response response = new Response();
		response.setFraudAlert(recentFraudList);
		logger.info("Sending to UI "+response);
		//send to ui
		this.template.convertAndSend("/topic/fraudData", response);
	}

	public class CustomComparator implements Comparator<FraudAlertData> {
		@Override
		public int compare(FraudAlertData o1, FraudAlertData o2) {
			return o2.getTrans_time().compareTo(o1.getTrans_time());

		}
	}
	
}
