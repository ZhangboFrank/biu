package bglutil.common;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;

public class CWUtil {
	public void printAllPhysicalId(AmazonCloudWatch cw){
		for(MetricAlarm ma:cw.describeAlarms().getMetricAlarms()){
			System.out.println("monitoring-alarm: "+ma.getAlarmArn());
		}
	}
}
