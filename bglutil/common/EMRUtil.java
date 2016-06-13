package bglutil.common;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.model.ClusterSummary;
import com.amazonaws.services.elasticmapreduce.model.SetTerminationProtectionRequest;
import com.amazonaws.services.elasticmapreduce.model.TerminateJobFlowsRequest;

public class EMRUtil {
	
	public void printAllPhysicalId(AmazonElasticMapReduce emr){
		for(ClusterSummary cs:emr.listClusters().getClusters()){
			if(!cs.getStatus().getState().equals("TERMINATED") && !cs.getStatus().getState().equals("TERMINATED_WITH_ERRORS")){
				System.out.println("emr: "+cs.getName()+", "+cs.getId()+", "+cs.getStatus().getState());
			}
		}
	}
	
	public List<ClusterSummary> getAliveEmrClusters(AmazonElasticMapReduce emr){
		ArrayList<ClusterSummary> list = new ArrayList<ClusterSummary>();
		for(ClusterSummary cs:emr.listClusters().getClusters()){
			if(!cs.getStatus().getState().equals("TERMINATED") && !cs.getStatus().getState().equals("TERMINATED_WITH_ERRORS")){
				list.add(cs);
			}
		}
		return list;
	}
	
	public void terminateEmrCluster(AmazonElasticMapReduce emr, String clusterId){
		emr.setTerminationProtection(new SetTerminationProtectionRequest().withJobFlowIds(clusterId).withTerminationProtected(false));
		System.out.println("=> Terminating EMR cluster "+clusterId);
		emr.terminateJobFlows(new TerminateJobFlowsRequest().withJobFlowIds(clusterId));
	}
}
