package bglutil.common;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.Stack;

public class CFNUtil {
	public void printAllPhysicalId(AmazonCloudFormation cfn){
		for(Stack stack:cfn.describeStacks().getStacks()){
			System.out.println("cloudformation-stack: "+stack.getStackName()+", "+stack.getStackId());
		}
	}
	
	public void deleteCNFStack(AmazonCloudFormation cfn, String stackName){
		DeleteStackRequest request = new DeleteStackRequest()
									.withStackName(stackName);
		System.out.println("=> Dropping CFN stack: "+stackName);
		cfn.deleteStack(request);
	}
}
