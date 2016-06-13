package bglutil.common;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.Cluster;

public class ECSUtil {
	public void printAllPhysicalId(AmazonECS ecs){
		for(Cluster cluster:ecs.describeClusters().getClusters()){
			System.out.println("ecs-cluster: "+cluster.getClusterName());
		}
	}
}
