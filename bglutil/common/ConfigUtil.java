package bglutil.common;

import com.amazonaws.services.config.AmazonConfig;
import com.amazonaws.services.config.model.ConfigurationRecorder;

public class ConfigUtil {
	public void printAllPhysicalId(AmazonConfig config){
		for(ConfigurationRecorder cr:config.describeConfigurationRecorders().getConfigurationRecorders()){
			System.out.println("config-recorder: "+cr.getName()+", role: "+cr.getRoleARN());
		}
	}
}
