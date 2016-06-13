package bglutil.common;

import com.amazonaws.services.codedeploy.AmazonCodeDeploy;

public class CodeDeployUtil {
	public void printAllPhysicalId(AmazonCodeDeploy codedeploy){
		for(String app:codedeploy.listApplications().getApplications()){
			System.out.println("codedeploy-app: "+app);
		}
	}
}
