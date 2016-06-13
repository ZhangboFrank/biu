package bglutil.common;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;

public class LambdaUtil {
	public void printAllPhysicalId(AWSLambda lambda){
		for(FunctionConfiguration fc:lambda.listFunctions().getFunctions()){
			System.out.println("lambda: "+fc.getFunctionName());
		}
	}
}
