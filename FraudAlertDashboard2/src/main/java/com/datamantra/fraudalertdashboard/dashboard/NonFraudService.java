package com.datamantra.fraudalertdashboard.dashboard;

import com.datamantra.fraudalertdashboard.dao.NonFraudDataRepository;
import com.datamantra.fraudalertdashboard.dao.entity.NonFraudData;
import com.datamantra.fraudalertdashboard.vo.ResponseNF;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/*
 * Service class to send fraud transaction messages to dashboard ui at fixed interval using web-socket.
 */
@Service
public class NonFraudService {
	private static final Logger logger = Logger.getLogger(NonFraudService.class);


	@Autowired
	private SimpMessagingTemplate template;
	
	@Autowired
	private NonFraudDataRepository nonFraudRepository;
	
	private long previous_timestamp=0L;
	
	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	//Method sends fraud transaction message in every 5 seconds.
	@Scheduled(fixedRate = 5000)
	public void trigger() {

		List<NonFraudData> recentNonFraudList = new ArrayList<NonFraudData>();
		//Call dao methods
		if (previous_timestamp==0) {
			nonFraudRepository.findFraudDataByTimestamp(new Date().getTime() - 10000).forEach(e -> {
				recentNonFraudList.add(e);
			});
			Collections.sort(recentNonFraudList, new CustomComparator());
			previous_timestamp = recentNonFraudList.get(0).getTrans_time().getTime();
		}
		else {
			nonFraudRepository.findFraudDataByTimestamp(previous_timestamp).forEach(e -> {
				recentNonFraudList.add(e);
			});
			Collections.sort(recentNonFraudList, new CustomComparator());
			previous_timestamp = recentNonFraudList.get(0).getTrans_time().getTime();
		}
		logger.info("previous_timestamp: " + previous_timestamp);


		//prepare response
		ResponseNF response = new ResponseNF();
		response.setNonFraudAlert(recentNonFraudList);
		logger.info("Sending to UI "+response);
		//send to ui
		this.template.convertAndSend("/topic/nonFraudData", response);
	}

	public class CustomComparator implements Comparator<NonFraudData> {
		@Override
		public int compare(NonFraudData o1, NonFraudData o2) {
			return o2.getTrans_time().compareTo(o1.getTrans_time());

		}
	}
	
}
