package bglutil.common;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;

public class EBUtil {
	public void printAllPhysicalId(AWSElasticBeanstalk eb){
		for(ApplicationDescription ad: eb.describeApplications().getApplications()){
			String app = ad.getApplicationName();
			StringBuffer sb = new StringBuffer();
			for(String version: ad.getVersions()){
				sb.append(", "+version);
			}
			System.out.println("elasticbeanstalk-app: "+app+new String(sb));
		}
	}
}
