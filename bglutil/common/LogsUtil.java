package bglutil.common;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.LogGroup;

public class LogsUtil {
	public void printAllPhysicalId(AWSLogs logs){
		for(LogGroup lg:logs.describeLogGroups().getLogGroups()){
			System.out.println("log-group: "+lg.getLogGroupName()+", "+lg.getArn());
		}
	}
}
