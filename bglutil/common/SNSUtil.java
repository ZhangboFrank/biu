package bglutil.common;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.Topic;

public class SNSUtil {
	
	public void printAllPhysicalId(AmazonSNS sns){
		for(Topic t: sns.listTopics().getTopics()){
			System.out.println("sns: "+t.getTopicArn());
		}
	}
	
	public void dropTopicByName(AmazonSNS sns, String accountId, Regions region, String name){
		String topicArn = region.equals(Regions.CN_NORTH_1)?
				"arn:aws-cn:sns:"+region.getName()+":"+accountId+":"+name:
				"arn:aws:sns:"+region.getName()+":"+accountId+":"+name;
		System.out.println("Deleting "+topicArn);
		DeleteTopicRequest request = new DeleteTopicRequest()
										.withTopicArn(topicArn);
		sns.deleteTopic(request);
	}
	
}
