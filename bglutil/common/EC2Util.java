package bglutil.common;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateNetworkAclEntryRequest;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteNetworkAclEntryRequest;
import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RuleAction;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.Vpc;

public class EC2Util {
		
	public void printAllPhysicalId(AmazonEC2 ec2){
		System.out.println("[] EC2 instances...");
		for(Reservation r:ec2.describeInstances().getReservations()){
			for(Instance i:r.getInstances()){
				System.out.print("ec2-instance: "+i.getInstanceId()+", "+i.getInstanceType()+", "+i.getSriovNetSupport()+", "+i.getState().getName()+", "+i.getPrivateIpAddress()+", "+i.getPublicIpAddress()+", ");
				for(Tag tag:i.getTags()){
					if(tag.getKey().equals("Name")){
						System.out.println(tag.getValue());
					}
				}
			}
		}
		System.out.println("[] Checking EIP...");
		for(Address address:ec2.describeAddresses().getAddresses()){
			System.out.println("eip-private-instnace: "+address.getPublicIp()+", "+address.getPrivateIpAddress()+", "+address.getInstanceId());
		}
		System.out.println("[] Checking Key pair...");
		for(KeyPairInfo kpi:ec2.describeKeyPairs().getKeyPairs()){
			System.out.println("ec2-keypair: "+kpi.getKeyName());
		}
		System.out.println("[] Checking AMI...");
		for(Image image:ec2.describeImages(new DescribeImagesRequest().withOwners("self")).getImages()){
			System.out.println("ec2-ami: "+image.getName()+", "+image.getImageId());
		}
		System.out.println("[] Checking EBS...");
		for(Volume v:ec2.describeVolumes().getVolumes()){
			System.out.println("ec2-ebs: "+v.getVolumeId()+", "+v.getVolumeType()+", "+v.getSize()+", "+v.getAvailabilityZone());
		}
	}
	
	public void clearOrphanSnapshot(AmazonEC2 ec2, String accountId){
		List<Volume> volumes = ec2.describeVolumes().getVolumes();
		List<String> ebsVolIds = new ArrayList<String>();
		for(Volume v:volumes){
			ebsVolIds.add(v.getVolumeId());
		}
		System.out.println("EBS Volumes Count: "+ebsVolIds.size());
		List<Image> images = ec2.describeImages(new DescribeImagesRequest().withOwners("self")).getImages();
		System.out.println("AMI Count: "+images.size());
		List<String> amiSnapshotIds = new ArrayList<String>();
		for(Image i:images){
			List<BlockDeviceMapping> blockDeviceMappings = i.getBlockDeviceMappings();
			for(BlockDeviceMapping bdm:blockDeviceMappings){
				EbsBlockDevice device = bdm.getEbs();
				if(device!=null){
					String ssid = device.getSnapshotId();
					amiSnapshotIds.add(ssid);
				}
			}
		}
		List<Snapshot> snapshots = ec2.describeSnapshots(new DescribeSnapshotsRequest().withOwnerIds(accountId)).getSnapshots();
		System.out.println("Snapshot Count: "+snapshots.size());
		String gone = null;
		for(Snapshot ss:snapshots){
			if(ebsVolIds.contains(ss.getVolumeId())){
				System.out.println(ss.getSnapshotId()+" is backup of "+ss.getVolumeId());
			}
			else if(amiSnapshotIds.contains(ss.getSnapshotId())){
				System.out.println(ss.getSnapshotId()+" is used by existing AMI");
			}
			else{
				gone = ss.getSnapshotId();
				System.out.println("Removing "+gone);
				ec2.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(gone));
			}
		}
	}
	
	public void dropSnapshot(AmazonEC2 ec2, String snapshotId){
		ec2.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(snapshotId));
	}
	
	//TODO
	/**
	 * @param ec2
	 * @param amiName
	 * @return updatedAmiId
	 */
	public String yumUpdateAmiByName(AmazonEC2 ec2, String amiName){
		return null;
	}
	
	public List<Image> getAllAmi(AmazonEC2 ec2){
		List<Image> images = ec2.describeImages().getImages();
		return images;
	}
	
	public List<Image> getAmiByName(AmazonEC2 ec2, String amiName){
		Filter f = new Filter().withName("name").withValues(amiName+"*");
		DescribeImagesResult result = ec2.describeImages(new DescribeImagesRequest().withFilters(f));
		return result.getImages();
	}
	
	public void deregisterAmi(AmazonEC2 ec2, String amiId){
		ec2.deregisterImage(new DeregisterImageRequest().withImageId(amiId));
		System.out.println("=> Deregisting AMI "+amiId);
	}
	
	public TreeSet<String> getDependentSecurityGroupIds(AmazonEC2 ec2, String securityGroupId){
		List<SecurityGroup> sgs = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(securityGroupId)).getSecurityGroups();
		ArrayList<String> dependentSgIds = new ArrayList<String>();
		for(SecurityGroup sg:sgs){
			List<IpPermission> ipPermissions = sg.getIpPermissions();
			List<IpPermission> ipPermissionsEgress = sg.getIpPermissionsEgress();
			for(IpPermission p:ipPermissions){
				List<UserIdGroupPair> userIdGroupPairs = p.getUserIdGroupPairs();
				for(UserIdGroupPair pair:userIdGroupPairs){
					//System.out.println(pair.getGroupId()+":"+pair.getVpcId()+":"+pair.getVpcPeeringConnectionId());
					dependentSgIds.add(pair.getGroupId());
				}
			}
			for(IpPermission p:ipPermissionsEgress){
				List<UserIdGroupPair> userIdGroupPairs = p.getUserIdGroupPairs();
				for(UserIdGroupPair pair:userIdGroupPairs){
					//System.out.println(pair.getGroupId()+":"+pair.getVpcId()+":"+pair.getVpcPeeringConnectionId());
					dependentSgIds.add(pair.getGroupId());
				}
			}
		}
		TreeSet<String> ts = new TreeSet<String>();
		ts.addAll(dependentSgIds);
		return ts;
	}
	
	public TreeSet<String> getReferencingSecurityGroupIds(AmazonEC2 ec2, String securityGroupId){
		ArrayList<String> referencingSgIds = new ArrayList<String>();
		SecurityGroup sg = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(securityGroupId)).getSecurityGroups().get(0);
		Filter f = new Filter().withName("vpc-id").withValues(sg.getVpcId());
		//System.out.println(sg.getVpcId());
		List<SecurityGroup> sgInSameVpc = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withFilters(f)).getSecurityGroups();
		for(SecurityGroup vpcSg:sgInSameVpc){
			//System.out.println(vpcSg.getGroupId());
			if(!(vpcSg.getGroupId().equals(securityGroupId))){
				//System.out.println(vpcSg.getGroupId());
				for(String depentId:this.getDependentSecurityGroupIds(ec2, vpcSg.getGroupId())){
					//System.out.println(depentId);
					if(depentId.equals(securityGroupId)){
						referencingSgIds.add(vpcSg.getGroupId());
					}
				}
			}
		}
		TreeSet<String> ts = new TreeSet<String>();
		ts.addAll(referencingSgIds);
		return ts;
	}
	
	public void terminateReservationWait(AmazonEC2 ec2, String reservationId){
		System.out.println("=> Dropping reservation id: "+reservationId);
		Filter f = new Filter().withName("reservation-id").withValues(reservationId);
		DescribeInstancesRequest request = new DescribeInstancesRequest()
											.withFilters(f);
		String nextToken = null;
		List<InstanceStateChange> isc = null;
		do{
			request.setNextToken(nextToken);
			DescribeInstancesResult result = ec2.describeInstances(request);
			List<Reservation> reservations = result.getReservations();
			for(Reservation r:reservations){
				List<Instance> instances = r.getInstances();
				ArrayList<String> instanceIds = new ArrayList<String>();
				for(Instance i:instances){
					System.out.println("Bye: "+i.getInstanceId());
					instanceIds.add(i.getInstanceId());
				}
				TerminateInstancesRequest tr = new TerminateInstancesRequest().withInstanceIds(instanceIds);
				ec2.terminateInstances(tr);
				System.out.println("Waiting for instance termination: ");
				int testCard = 0;
				while(true){
					this.wait(5000);
					isc = ec2.terminateInstances(tr).getTerminatingInstances();
					testCard = 0;
					for(InstanceStateChange sc:isc){
						System.out.println(sc.getInstanceId()+" "+sc.getCurrentState());
						if(!sc.getCurrentState().getName().equals(InstanceStateName.Terminated.toString())){
							break;
						}
						testCard++;
					}
					if(isc.size()==testCard){break;}
				}
			}
			nextToken = result.getNextToken();
		}while(nextToken!=null);
	}
	
	private void wait(int m){
		try {
			Thread.sleep(m);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Terminate EC2 instances by given reservation id.
	 * @param ec2
	 * @param reservationId
	 */
	public void terminateReservation(AmazonEC2 ec2, String reservationId){
		System.out.println("=> Dropping reservation id: "+reservationId);
		Filter f = new Filter().withName("reservation-id").withValues(reservationId);
		DescribeInstancesRequest request = new DescribeInstancesRequest()
											.withFilters(f);
		String nextToken = null;
		do{
			request.setNextToken(nextToken);
			DescribeInstancesResult result = ec2.describeInstances(request);
			List<Reservation> reservations = result.getReservations();
			for(Reservation r:reservations){
				List<Instance> instances = r.getInstances();
				ArrayList<String> instanceIds = new ArrayList<String>();
				for(Instance i:instances){
					System.out.println("Bye: "+i.getInstanceId());
					instanceIds.add(i.getInstanceId());
				}
				TerminateInstancesRequest tr = new TerminateInstancesRequest().withInstanceIds(instanceIds);
				ec2.terminateInstances(tr);			
			}
			nextToken = result.getNextToken();
		}while(nextToken!=null);										
	}
	
	/**
	 * Terminate EC2 instances by given filter.
	 * @param ec2
	 * @param filter
	 */
	public void terminateInstancesByFilter(AmazonEC2 ec2, Filter filter){
		DescribeInstancesRequest request = new DescribeInstancesRequest()
											.withFilters(filter);
		/*
		for(String filterValue:filter.getValues()){
			System.out.println("Target: "+filterValue);
		}*/
		String nextToken = null;
		do{
			request.setNextToken(nextToken);
			DescribeInstancesResult result = ec2.describeInstances(request);
			for(Reservation r:result.getReservations()){
				this.terminateReservationWait(ec2, r.getReservationId());
			}
			nextToken = result.getNextToken();
		}while(nextToken!=null);
	}
	
	/**
	 * Drop security group by given filter.
	 * @param ec2
	 * @param filter
	 */
	public void dropSecurityGroupByFilter(AmazonEC2 ec2, Filter filter){
		DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
												.withFilters(filter);
		DescribeSecurityGroupsResult result = ec2.describeSecurityGroups(request);
		
		DeleteSecurityGroupRequest dRequest = new DeleteSecurityGroupRequest();
		
		for(SecurityGroup sg:result.getSecurityGroups()){
			System.out.println("=> Dropping "+sg.getGroupId()+": "+sg.getGroupName());
			dRequest.setGroupId(sg.getGroupId());
			try{
				ec2.deleteSecurityGroup(dRequest);
			}catch(AmazonServiceException ex){
				System.out.println(ex.getMessage());
			}
		}
	}

	public void dropEbsByFilter(AmazonEC2 ec2, Filter filter){
		DescribeVolumesRequest request = new DescribeVolumesRequest()
											.withFilters(filter);
		/*
		for(String filterValue:filter.getValues()){
			System.out.println("Target: "+filterValue);
		}*/
		String nextToken = null;
		do{
			request.setNextToken(nextToken);
			DescribeVolumesResult result = ec2.describeVolumes(request);
			for(Volume vol:result.getVolumes()){
				System.out.println("=> Deleting "+vol.getVolumeId());
				ec2.deleteVolume(new DeleteVolumeRequest().withVolumeId(vol.getVolumeId()));
			}
			nextToken = result.getNextToken();
		}while(nextToken!=null);
	}
	
	public void dropSnapshotByFilter(AmazonEC2 ec2, Filter filter){
		DescribeSnapshotsRequest request = new DescribeSnapshotsRequest()
											.withFilters(filter);
		/*
		for(String filterValue:filter.getValues()){
			System.out.println("Target: "+filterValue);
		}*/
		String nextToken = null;
		do{
			request.setNextToken(nextToken);
			DescribeSnapshotsResult result = ec2.describeSnapshots(request);
			for(Snapshot ss:result.getSnapshots()){
				System.out.println("=> Deleting "+ss.getSnapshotId());
				ec2.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(ss.getSnapshotId()));
			}
			nextToken = result.getNextToken();
		}while(nextToken!=null);
	}
	
	/**
	 * Adding rule# 49 to deny all ingress on ncal.
	 * @param ec2
	 * @param naclId
	 */
	public void denyAllIngressOnNACL(AmazonEC2 ec2, String naclId){
		CreateNetworkAclEntryRequest newEntryRequest = new CreateNetworkAclEntryRequest()
		.withRuleAction(RuleAction.Deny)
		.withCidrBlock("0.0.0.0/0")
		.withProtocol("-1")
		.withRuleNumber(49)
		.withNetworkAclId(naclId)
		.withEgress(false);
		ec2.createNetworkAclEntry(newEntryRequest);
		System.out.println("Adding deny rule entry for "+naclId);
	}
	
	/**
	 * Removing rule#49 from ingress on ncal.
	 * @param ec2
	 * @param naclId
	 */
	public void removeIngressNo49(AmazonEC2 ec2, String naclId){
		DeleteNetworkAclEntryRequest d = new DeleteNetworkAclEntryRequest()
		.withNetworkAclId(naclId)
		.withRuleNumber(49)
		.withEgress(false);
		ec2.deleteNetworkAclEntry(d);
		System.out.println("Removing deny rule entry for "+naclId);
	}
	
	public void dropKeypair(AmazonEC2 ec2, String keyName){
		ec2.deleteKeyPair(new DeleteKeyPairRequest().withKeyName(keyName));
		System.out.println("=> Deleting KeyPair: "+keyName);
	}
	
	public void dropVpcByFilter(AmazonEC2 ec2, Filter filter){
		DescribeVpcsRequest dr = new DescribeVpcsRequest()
									.withFilters(filter);
		DescribeVpcsResult result = ec2.describeVpcs(dr);
		for(Vpc vpc:result.getVpcs()){
			System.out.println("=> Deleting vpc: "+vpc.getVpcId());
			Filter filterForVpc = new Filter().withName("vpc-id").withValues(vpc.getVpcId());
			for(Subnet subnet:ec2.describeSubnets(new DescribeSubnetsRequest().withFilters(filterForVpc)).getSubnets()){
				System.out.println("=> Deleting depending subnet: "+subnet.getSubnetId());
				ec2.deleteSubnet(new DeleteSubnetRequest().withSubnetId(subnet.getSubnetId()));
			}
			for(RouteTable rt:ec2.describeRouteTables(new DescribeRouteTablesRequest().withFilters(filterForVpc)).getRouteTables()){
				if(rt.getAssociations().size()==0){
					System.out.println("=> Deleting depending route table: "+rt.getRouteTableId());
					ec2.deleteRouteTable(new DeleteRouteTableRequest().withRouteTableId(rt.getRouteTableId()));
				}
			}
			Filter filterForVpcIgw = new Filter().withName("attachment.vpc-id").withValues(vpc.getVpcId());
			for(InternetGateway igw: ec2.describeInternetGateways(new DescribeInternetGatewaysRequest().withFilters(filterForVpcIgw)).getInternetGateways()){
				System.out.println("=> Deleting attached igw: "+igw.getInternetGatewayId());
				ec2.detachInternetGateway(new DetachInternetGatewayRequest().withInternetGatewayId(igw.getInternetGatewayId()).withVpcId(vpc.getVpcId()));
				ec2.deleteInternetGateway(new DeleteInternetGatewayRequest().withInternetGatewayId(igw.getInternetGatewayId()));
			}
			for(SecurityGroup sg:ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withFilters(filterForVpc)).getSecurityGroups()){
				if(!sg.getGroupName().equals("default")){
					System.out.println("=> Deleting depending security group: "+sg.getGroupId());
					ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupId(sg.getGroupId()));
				}
			}
			try {
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				// Will not reach here.
				e.printStackTrace();
			}
			ec2.deleteVpc(new DeleteVpcRequest().withVpcId(vpc.getVpcId()));
		}
	}
	
	public String runInstance(AmazonEC2 ec2, String imageId, String keyName, String subnetId, String backupSubnetId, String securityGroupId, String userData, String iamInstanceProfile){
		// ami-baada8e8 the proxy server in Singapore.
		InstanceType instanceType = InstanceType.T2Micro;
		int minCount = 1;
		RunInstancesRequest runRequest = null;
		try{
			runRequest = new RunInstancesRequest()
											.withInstanceInitiatedShutdownBehavior("stop")
											.withImageId(imageId)
											.withInstanceType(instanceType)
											.withMinCount(minCount)
											.withMaxCount(1)
											.withKeyName(keyName)
											.withSubnetId(subnetId)
											.withUserData(userData)
											.withIamInstanceProfile(new IamInstanceProfileSpecification().withName(iamInstanceProfile));
		}catch(AmazonServiceException ex){
			runRequest = new RunInstancesRequest()
			.withInstanceInitiatedShutdownBehavior("stop")
			.withImageId(imageId)
			.withInstanceType(instanceType)
			.withMinCount(minCount)
			.withMaxCount(1)
			.withKeyName(keyName)
			.withSubnetId(backupSubnetId)
			.withUserData(userData)
			.withIamInstanceProfile(new IamInstanceProfileSpecification().withName(iamInstanceProfile));
		}
		
		System.out.print("Sending request to run instance ...");
		RunInstancesResult result = ec2.runInstances(runRequest);
		System.out.println(result);
		Reservation reservation = result.getReservation();
		List<Instance> instances = reservation.getInstances();
		List<String> instanceIds = new ArrayList<String>();
		String instanceId = instances.get(0).getInstanceId();
		for(Instance instance:instances){
			instanceIds.add(instance.getInstanceId());
			ModifyInstanceAttributeRequest miar = new ModifyInstanceAttributeRequest()
			.withGroups(securityGroupId)
			.withInstanceId(instance.getInstanceId());
			ec2.modifyInstanceAttribute(miar);
		}
		System.out.println(instanceIds);
		DescribeInstanceStatusRequest descRequest = new DescribeInstanceStatusRequest()
														.withInstanceIds(instanceIds);
		DescribeInstanceStatusResult descResult = null;
		List<InstanceStatus> instanceStatuses = null;
		List<String> instanceStatusesStr = null;
		boolean con = true;
		int tn = 0;
		while(con){
			try {
				Thread.sleep(5*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			descResult = ec2.describeInstanceStatus(descRequest);
			instanceStatuses = descResult.getInstanceStatuses();
			instanceStatusesStr = new ArrayList<String>();
			for(InstanceStatus instanceStatus: instanceStatuses){
				System.out.println("#"+(++tn)+": "+instanceStatus.getInstanceStatus());
				instanceStatusesStr.add(instanceStatus.getInstanceStatus().getStatus());
			}
			
			if(instanceStatusesStr!=null && instanceStatusesStr.size()>= minCount && !instanceStatusesStr.contains(new String("initializing"))){
				con = false;
			}
		}
		System.out.println("Done.");
		return instanceId;
	}
	
}
