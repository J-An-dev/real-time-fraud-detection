import requests
import json
import subprocess
import sys, os
from os.path import expanduser

home = expanduser("~")
def stopStartStreamingJob():
    stop_streaming = 'touch /tmp/shutdownmarker'
    os.system(stop_streaming)
    shutdown_flag = False
  
    while not shutdown_flag:
        URL = "http://localhost:8080/json"
        r = requests.get(url = URL)
        data = json.loads(r.content)
        activeapps = data['activeapps']
        if not activeapps:
            print("No Active apps, Streaming app is shutdown")
            shutdown_flag = True
        else:
            print("List is not empty")
            for app in activeapps:
                if not app['name'] == 'RealTime Creditcard FraudDetection':
                    print("Streaming Job is still running")
                    continue
                else:
                    shutdown_flag = True
                    break

    remove_shutdown_marker = 'rm -rf /tmp/shutdownmarker'
    os.system(remove_shutdown_marker)

    start_streaming = 'spark-submit --class com.datamantra.spark.jobs.RealTimeFraudDection --name "RealTime Creditcard FraudDetection" --master spark://datamantra:7077' + ' '  + '--deploy-mode cluster' + ' ' + home + '/frauddetection/spark/fruaddetection-spark.jar' + ' ' + home + '/frauddetection/spark/application-local.conf &'
    os.system(start_streaming)

if __name__ == '__main__':
    stopStartStreamingJob()
