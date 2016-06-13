package bglutil.common;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;

public class SQSUtil{
	
	public void printAllPhysicalId(AmazonSQS sqs){
		for(String url:sqs.listQueues().getQueueUrls()){
			System.out.println("sqs: "+url);
		}
	}
	
	
	public void dropQueueByName(AmazonSQS sqs, String accountId, Regions region, String name){
		
		String queueUrl = region.equals(Regions.CN_NORTH_1)?
				"https://sqs."+region.getName()+".amazonaws.com.cn/"+accountId+"/"+name:
				"https://sqs."+region.getName()+".amazonaws.com/"+accountId+"/"+name;	
		
		System.out.println("Deleting "+queueUrl);
		DeleteQueueRequest request = new DeleteQueueRequest()
										.withQueueUrl(queueUrl);
		
		sqs.deleteQueue(request);
	}
}
