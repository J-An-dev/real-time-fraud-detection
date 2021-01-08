package com.datamantra.fraudalertdashboard.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring boot application class for Dashboard.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.datamantra.fraudalertdashboard.dashboard", "com.datamantra.fraudalertdashboard.dao"})
public class FraudAlertDashboard {

	  public static void main(String[] args)
	  {
		  SpringApplication.run(FraudAlertDashboard.class, args);
	  }
}

