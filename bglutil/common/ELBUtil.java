package bglutil.common;

import java.util.List;


import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;

public class ELBUtil {
	
	public void printAllPhysicalId(AmazonElasticLoadBalancing elb){
		for(LoadBalancerDescription desc:elb.describeLoadBalancers().getLoadBalancerDescriptions()){
			System.out.println("elb: "+desc.getLoadBalancerName()+", dns: "+desc.getDNSName());
		}
		
	}
	
	public void describeInstanceHealthByElbName(AmazonElasticLoadBalancing elb, String loadBalancerName){
		List<InstanceState> instanceStates = elb.describeInstanceHealth(new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancerName)).getInstanceStates();
		for(InstanceState is:instanceStates){
			System.out.println(is.getInstanceId()+": "+is.getState()+", "+is.getReasonCode()+", "+is.getDescription());
		}
	}
	
	public void deleteElbByName(AmazonElasticLoadBalancing elb, String name){
		String dns = elb.describeLoadBalancers(new DescribeLoadBalancersRequest().withLoadBalancerNames(name)).getLoadBalancerDescriptions().get(0).getDNSName();		
		DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest()
		.withLoadBalancerName(name);
		elb.deleteLoadBalancer(request);
		System.out.println("=> Deleting "+name+" with DNS "+dns);
	}
	
}
