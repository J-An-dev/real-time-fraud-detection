#!/usr/bin/env bash
#Dstream Streaming Spark Job
spark-submit --class com.datamantra.spark.jobs.RealTimeFraudDetection.DstreamFraudDetection --name "RealTime Creditcard FraudDetection Dstream" --master spark://datamantra:6066 --deploy-mode cluster  $HOME/frauddetection/spark/fruaddetection-spark.jar $HOME/frauddetection/spark/application-local.conf
