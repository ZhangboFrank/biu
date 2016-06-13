package bglutil.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import sun.misc.BASE64Decoder;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;

public class ASGUtil {
	
	public void printAllPhysicalId(AmazonAutoScaling asg) {
		for (AutoScalingGroup group : asg.describeAutoScalingGroups()
				.getAutoScalingGroups()) {
			System.out.println("asg: " + group.getAutoScalingGroupName());
		}
		for (LaunchConfiguration lc : asg.describeLaunchConfigurations()
				.getLaunchConfigurations()) {
			System.out.println("asg-lc: " + lc.getLaunchConfigurationName());
		}
	}
	
	public Map<String,String> getLaunchConfigSideBySide(AmazonAutoScaling asg, String lc1Name, String lc2Name){
		Hashtable<String,String> diff = new Hashtable<String,String>();
		LaunchConfiguration lc1 = asg.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(lc1Name)).getLaunchConfigurations().get(0);
		LaunchConfiguration lc2 = asg.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(lc2Name)).getLaunchConfigurations().get(0);
		diff.put("AssociatePublicIpAddress",lc1.getAssociatePublicIpAddress()+":"+lc2.getAssociatePublicIpAddress());
		diff.put("EbsOptimized",lc1.getEbsOptimized()+":"+lc2.getEbsOptimized());
		diff.put("IamInstanceProfile",lc1.getIamInstanceProfile()+":"+lc2.getIamInstanceProfile());
		diff.put("ImageId",lc1.getImageId()+":"+lc2.getImageId());
		diff.put("InstanceType",lc1.getInstanceType()+":"+lc2.getInstanceType());
		diff.put("Monitoring",lc1.getInstanceMonitoring().getEnabled()+":"+lc2.getInstanceMonitoring().getEnabled());
		diff.put("KernelId",lc1.getKernelId()+":"+lc2.getKernelId());
		diff.put("KeyName",lc1.getKeyName()+":"+lc2.getKeyName());
		diff.put("Tenancy",lc1.getPlacementTenancy()+":"+lc2.getPlacementTenancy());
		diff.put("RamDiskId", lc1.getRamdiskId()+":"+lc2.getRamdiskId());
		diff.put("SpotPrice",lc1.getSpotPrice()+":"+lc2.getSpotPrice());
		ArrayList<BlockDeviceMapping> lc1bdm = new ArrayList<BlockDeviceMapping>();
		ArrayList<BlockDeviceMapping> lc2bdm = new ArrayList<BlockDeviceMapping>();
		for(BlockDeviceMapping bdm:lc1.getBlockDeviceMappings()){
			lc1bdm.add(bdm);
		}
		for(BlockDeviceMapping bdm:lc2.getBlockDeviceMappings()){
			lc2bdm.add(bdm);
		}
		StringBuffer lc1BdmFormatted = new StringBuffer();
		StringBuffer lc2BdmFormatted = new StringBuffer();
		for(BlockDeviceMapping bdm:lc1bdm){
			lc1BdmFormatted.append(bdm.getVirtualName()+">"+bdm.getDeviceName()+">"+bdm.getEbs().getSnapshotId()+">"+bdm.getEbs().getVolumeType()+">"+bdm.getEbs().getVolumeSize()+">"+bdm.getEbs().getIops()+">"+(bdm.getEbs().getDeleteOnTermination()?"DeleteOnTerm":"PreserveOnTerm")+",");
		}
		for(BlockDeviceMapping bdm:lc2bdm){
			lc2BdmFormatted.append(bdm.getVirtualName()+">"+bdm.getDeviceName()+">"+bdm.getEbs().getSnapshotId()+">"+bdm.getEbs().getVolumeType()+">"+bdm.getEbs().getVolumeSize()+">"+bdm.getEbs().getIops()+">"+(bdm.getEbs().getDeleteOnTermination()?"DeleteOnTerm":"PreserveOnTerm")+",");
		}
		diff.put("BlockDeviceMapping",new String(lc1BdmFormatted)+":"+new String(lc2BdmFormatted));
		TreeSet<String> lc1Sg = new TreeSet<String>();
		TreeSet<String> lc2Sg = new TreeSet<String>();
		for(String sgName:lc1.getSecurityGroups()){
			lc1Sg.add(sgName);
		}
		for(String sgName:lc2.getSecurityGroups()){
			lc2Sg.add(sgName);
		}
		StringBuffer lc1SgFormatted = new StringBuffer();
		StringBuffer lc2SgFormatted = new StringBuffer();
		for(String sgName:lc1Sg){
			lc1SgFormatted.append(sgName+",");
		}
		for(String sgName:lc2Sg){
			lc2SgFormatted.append(sgName+",");
		}
		diff.put("SecurityGroups", new String(lc1SgFormatted)+":"+new String(lc2SgFormatted));
		String lc1Userdata = this.base64Decode(lc1.getUserData());
		String lc2Userdata = this.base64Decode(lc2.getUserData());
		diff.put("UserData for "+lc1Name, lc1Userdata);
		diff.put("UserData for "+lc2Name, lc2Userdata);
		return diff;
	}
	
	private String base64Decode(String base64){
		byte[] b = null;
		String plain = null;
		if(base64 != null){
			BASE64Decoder decoder = new BASE64Decoder();
			try {
				b = decoder.decodeBuffer(base64);
				plain = new String(b, "utf-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return plain;
	}
	
	private String getCurrentLaunchConfig(AmazonAutoScaling asg, String asgName){
		return asg.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asgName))
		.getAutoScalingGroups().get(0).getLaunchConfigurationName();
	}
	
	private String versionNumberIncrease(String currentName) {
		String versionPart = currentName.substring(currentName
				.lastIndexOf("-v"));
		String currentVersionNumber = versionPart.replace("-v", "");
		String nextVersionNumber = String.valueOf(Integer
				.valueOf(currentVersionNumber) + 1);
		System.out.println("Next version number is: #" + nextVersionNumber);
		String newName = currentName.replace("-v"+currentVersionNumber,
				"-v"+nextVersionNumber);
		return newName;
	}
	
	public boolean checkNewLaunchConfigAvail(AmazonAutoScaling asg, String asgName){
		String currentLc = this.getCurrentLaunchConfig(asg, asgName);
		String nextLc = this.versionNumberIncrease(currentLc);
		if(asg.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest()
											.withLaunchConfigurationNames(nextLc)).getLaunchConfigurations().size()==0){
			System.out.println(nextLc+" name space available...");
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Update (yum -y update) and swap stateless instances in an ASG softly.
	 * 
	 * @param asg
	 * @param asgName
	 * @param commandsToAmi [0] and [1] must be ASG name and Swap wait seconds
	 */
	public void commandsToAmiForAsgByName(AmazonAutoScaling aas, AmazonEC2 ec2,
			String keyName,
			String asgName, String amiBakerSubnetId, String amiBakerBackupSubnetId,
			String amiBakerSecurityGroupId,
			String[] commandsToAmi, int waitMins, boolean dryRun) {
		if(!commandsToAmi[0].equals(asgName) || !commandsToAmi[1].equals(Integer.toString(waitMins))){
			System.out.println("Parameter commandsToAmi not handled correctly.");
			return;
		}
		String userdataPrefix = 
				"#!/bin/bash\n"
				+ "rm -f /var/www/html/index-test.html\n"
				+ "yum -y install perl-libwww-perl\n" 
				+ "yum -y install httpd\n"
				+ "chkconfig httpd off\n"
				+ "service httpd stop\n"
				+ "yum -y update";
		String userdataSuffix =  " && echo \"_SUCCESS\" > /var/www/html/index-test.html && chmod a+r /var/www/html/index-test.html && service httpd restart\n";
		StringBuffer commandsBuffer = new StringBuffer();
		StringBuffer cBuffer = new StringBuffer();
		String c1 = null;
		boolean commandEnd = false;
		for(int i=2;i<commandsToAmi.length;i++){
			if(commandsToAmi[i].startsWith("\"")){
				cBuffer = new StringBuffer();
				c1 = commandsToAmi[i].replaceFirst("\"", "");
				cBuffer.append(c1+" ");
				commandEnd = false;
			}
			else if (commandsToAmi[i].endsWith("\"")){
				c1 = commandsToAmi[i].replaceAll("\"", "");
				cBuffer.append(c1+" ");
				commandsBuffer.append(" && "+new String(cBuffer));
				commandEnd = true;
			}
			else{
				c1 = commandsToAmi[i];
				cBuffer.append(c1+" ");
			}
			if(c1!=null){c1=null;}
			// commandsBuffer.append(" && "+commandsToAmi[i]); OLD
		}
		if(!commandEnd){
			System.out.println("Command NOT ended properly: "+new String(commandsBuffer));
			return;
		}
		String userdata = userdataPrefix+new String(commandsBuffer)+userdataSuffix;
		System.out.println(userdata);
		if(dryRun){
			System.out.println("Dry Run!! Over.");
			return;
		}
		// Step 1: Start a new instance use current AMI.
		System.out.println("Start a new instance use current AMI for " + asgName);
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		List<AutoScalingGroup> asgs = res.getAutoScalingGroups();
		if (asgs == null || asgs.size() != 1) {
			System.out.println("No such auto scaling group: " + asgName);
			System.exit(1);
		} else {
			AutoScalingGroup asg = asgs.get(0);
			String lcName = asg.getLaunchConfigurationName();
			if (lcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			DescribeLaunchConfigurationsRequest req2 = new DescribeLaunchConfigurationsRequest()
					.withLaunchConfigurationNames(lcName).withMaxRecords(1);
			DescribeLaunchConfigurationsResult res2 = aas
					.describeLaunchConfigurations(req2);
			List<LaunchConfiguration> lcs = res2.getLaunchConfigurations();
			if (lcs == null || lcs.size() != 1) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			} else {
				String iamInstanceProfile = lcs.get(0).getIamInstanceProfile();
				String currentAmiId = lcs.get(0).getImageId();
				String currentAmiName = ec2.describeImages(new DescribeImagesRequest().withImageIds(currentAmiId)).getImages().get(0).getName();
				EC2Util ec2u = new EC2Util();
				System.out.println("Create AMI baker using userdata:");
				System.out.println(userdata);
				String amiBakerInstanceId = ec2u.runInstance(ec2, currentAmiId,
						keyName, amiBakerSubnetId, amiBakerBackupSubnetId, amiBakerSecurityGroupId, GeneralUtil.base64Encode(userdata), iamInstanceProfile);
				DescribeInstancesRequest dir = new DescribeInstancesRequest()
						.withInstanceIds(amiBakerInstanceId);
				String reservationId = ec2.describeInstances(dir)
						.getReservations().get(0).getReservationId();
				String publicDnsName = ec2.describeInstances(dir).getReservations()
						.get(0).getInstances().get(0).getPublicDnsName();
				ArrayList<URL> urls = new ArrayList<URL>();
				try {
					urls.add(new URL("http://" + publicDnsName + "/index-test.html"));
				} catch (MalformedURLException e) {
					// Will NOT happen.
					e.printStackTrace();
				}
				// Wait for the 80 port to open.
				boolean portNotOpen = true;
				int maxRetry = 300;
				while (portNotOpen) {
					try {
						System.out.println("Try "+urls.get(0).toString());
						BufferedReader br = new BufferedReader(new InputStreamReader(urls
								.get(0).openConnection().getInputStream()));
						String lineOne = br.readLine();
						System.out.println(lineOne);
						br.close();
						portNotOpen = false;
					} catch (IOException e) {
						// e.printStackTrace();
						System.out
								.println("Port 80 not open yet, waiting and retry..."
										+ --maxRetry + " left.");
						if (maxRetry == 0) {
							System.out
									.println("*WARNING* Port 80 still NOT open after maximum retries. Delivery aborted.");
							ec2u.terminateReservation(ec2, reservationId);
							break;
						}
					} finally {
						try {
							Thread.sleep(10 * 1000);
						} catch (InterruptedException e) {
							// Will NOT happen.
							e.printStackTrace();
						}
					}
				}
				if (portNotOpen) {
					System.out.println("Delveiry aborted.");
					System.exit(-1);
				} else {
					// Step 2: Create a new AMI from step 1.
					System.out.println("Create a new AMI for " + asgName);
					CreateImageRequest createImageRequest = new CreateImageRequest()
							.withDescription("[=YumAutoUpdater=]")
							.withInstanceId(amiBakerInstanceId)
							.withNoReboot(false)
							.withName(
									GeneralUtil.versionNumberIncrease(currentAmiName));
					String newAmiId = ec2.createImage(
							createImageRequest).getImageId();
					
					String imageState = "pending";
					while (!imageState.equals("available")) {
						imageState = ec2
								.describeImages(
										new DescribeImagesRequest()
												.withImageIds(newAmiId))
								.getImages().get(0).getState();
						System.out.println("New AMI creation state: "
								+ imageState);
						try {
							Thread.sleep(1000 * 10);
						} catch (InterruptedException e) {
							// Will NOT reach here.
							e.printStackTrace();
						}
					}
					// Step 3: Discard AMI baker.
					System.out.println("Discard AMI baker by reserviation: "+reservationId);
					ec2u.terminateReservation(ec2, reservationId);
					// Step 4: Update ASG with new AMI from step 2 softly.
					System.out.println("Update " + asgName+" to use new AMI");
					this.changeAMIForAsg(aas, asgName, newAmiId, false, waitMins);
				}
			}
		}
	}

	public void deleteAsgByName(AmazonAutoScaling asg, String asgName) {
		asg.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest()
				.withForceDelete(true).withAutoScalingGroupName(asgName));
		System.out.println("=> Deleting ASG " + asgName + " requested");
	}

	public void deleteLcByName(AmazonAutoScaling asg, String lcName) {
		asg.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest()
				.withLaunchConfigurationName(lcName));
		System.out.println("=> Deleting LC " + lcName + " requested");
	}

	/**
	 * Create a new LC with a new userdata.
	 * 
	 * @param aas
	 * @param oldLcName
	 * @param userdataBase64
	 * @return
	 */
	public String createNewLaunchConfigWithUpdatedUserdata(
			AmazonAutoScaling aas, String oldLcName, String userdataBase64) {

		DescribeLaunchConfigurationsRequest req = new DescribeLaunchConfigurationsRequest()
				.withLaunchConfigurationNames(oldLcName).withMaxRecords(1);
		DescribeLaunchConfigurationsResult res = aas
				.describeLaunchConfigurations(req);
		List<LaunchConfiguration> lcs = res.getLaunchConfigurations();

		if (lcs == null || lcs.size() != 1) {
			return null;
		} else {
			LaunchConfiguration oldLc = lcs.get(0);
			String versionPart = oldLcName.substring(oldLcName
					.lastIndexOf("-v"));
			String currentVersionNumber = versionPart.replace("-v", "");
			String nextVersionNumber = String.valueOf(Integer
					.valueOf(currentVersionNumber) + 1);
			System.out.println("Next version number is: #" + nextVersionNumber);
			String newLcName = oldLcName.replace(currentVersionNumber,
					nextVersionNumber);
			CreateLaunchConfigurationRequest clcr = new CreateLaunchConfigurationRequest()
					.withAssociatePublicIpAddress(
							oldLc.getAssociatePublicIpAddress())
					.withBlockDeviceMappings(oldLc.getBlockDeviceMappings())
					.withEbsOptimized(oldLc.getEbsOptimized())
					.withIamInstanceProfile(oldLc.getIamInstanceProfile())
					.withImageId(oldLc.getImageId())
					.withInstanceMonitoring(oldLc.getInstanceMonitoring())
					.withInstanceType(oldLc.getInstanceType())
					.withKeyName(oldLc.getKeyName())
					.withLaunchConfigurationName(newLcName)
					.withPlacementTenancy(oldLc.getPlacementTenancy())
					.withSecurityGroups(oldLc.getSecurityGroups())
					.withSpotPrice(oldLc.getSpotPrice())
					.withUserData(userdataBase64);
			String kernelId = oldLc.getKernelId();
			if (kernelId != null && kernelId.length() != 0) {
				clcr.setKernelId(kernelId);
			}
			String ramdiskId = oldLc.getRamdiskId();
			if (ramdiskId != null && ramdiskId.length() != 0) {
				clcr.setRamdiskId(ramdiskId);
			}

			aas.createLaunchConfiguration(clcr);
			System.out.println(newLcName + " creation requested.");
			return newLcName;
		}
	}

	/**
	 * Change the userdata for existing ASG. Soft mode for rolling update.
	 * 
	 * @param aas
	 * @param asgName
	 * @param userdataBase64
	 * @param hard
	 */
	public void changeUserdataForAsg(AmazonAutoScaling aas, String asgName,
			String userdataBase64, boolean hard, int waitMins) {
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		List<AutoScalingGroup> asgs = res.getAutoScalingGroups();
		if (asgs == null || asgs.size() != 1) {
			System.out.println("No such auto scaling group: " + asgName);
			System.exit(1);
		} else {
			AutoScalingGroup asg = asgs.get(0);
			String lcName = asg.getLaunchConfigurationName();
			String newLcName = this.createNewLaunchConfigWithUpdatedUserdata(
					aas, lcName, userdataBase64);
			if (newLcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			UpdateAutoScalingGroupRequest uasgr = new UpdateAutoScalingGroupRequest()
					.withAutoScalingGroupName(asgName)
					.withLaunchConfigurationName(newLcName);
			aas.updateAutoScalingGroup(uasgr);
			System.out.println(asgName + " change to use " + newLcName);
			if (hard) {
				terminateInstancesInAsgAtOnce(aas, asgName);
			} else {
				terminateInstancesInAsgRolling(aas, asgName, waitMins);
			}
			this.deleteObsoleteLaunchConfig(aas, newLcName);
		}
		System.out.println("Change userdata for ASG completed.");
	}

	private void deleteObsoleteLaunchConfig(AmazonAutoScaling aas,
			String currentLcName) {

		String currentLcVersionPart = currentLcName.substring(currentLcName
				.lastIndexOf("-v"));
		int currentLcVersionNumber = Integer.valueOf((currentLcVersionPart
				.replace("-v", "")));
		int obsoleteLcVersionNumber = currentLcVersionNumber - 2;
		if (obsoleteLcVersionNumber > 0) {
			String obsoleteLcName = currentLcName.replace(
					String.valueOf(currentLcVersionNumber),
					String.valueOf(obsoleteLcVersionNumber));
			if (aas.describeLaunchConfigurations(
					new DescribeLaunchConfigurationsRequest()
							.withLaunchConfigurationNames(obsoleteLcName))
					.getLaunchConfigurations().size() > 0) {
				System.out.println("Obsolete launch config exists: "
						+ obsoleteLcName);
				DeleteLaunchConfigurationRequest dlcr = new DeleteLaunchConfigurationRequest()
						.withLaunchConfigurationName(obsoleteLcName);
				aas.deleteLaunchConfiguration(dlcr);
				System.out.println("Old launch config " + obsoleteLcName
						+ " deletion requested.");
			}

		}
	}

	/**
	 * Kill all instances within an ASG at once.
	 * 
	 * @param aas
	 * @param asgName
	 */
	public void terminateInstancesInAsgAtOnce(AmazonAutoScaling aas,
			String asgName) {

		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);

		List<Instance> instances = res.getAutoScalingGroups().get(0)
				.getInstances();

		TerminateInstanceInAutoScalingGroupRequest tiiasgr = null;
		String instanceId = null;
		for (Instance instance : instances) {
			instanceId = instance.getInstanceId();
			tiiasgr = new TerminateInstanceInAutoScalingGroupRequest()
					.withInstanceId(instanceId)
					.withShouldDecrementDesiredCapacity(false);
			aas.terminateInstanceInAutoScalingGroup(tiiasgr);
			System.out.println("Instance: " + instanceId
					+ " termination requested.");
		}

	}

	/**
	 * Create a new LC with a new AMI.
	 * 
	 * @param aas
	 * @param oldLcName
	 * @param newAMI
	 * @return
	 */
	public String createNewLaunchConfigWithNewAMI(AmazonAutoScaling aas,
			String oldLcName, String newAMI) {
		DescribeLaunchConfigurationsRequest req = new DescribeLaunchConfigurationsRequest()
				.withLaunchConfigurationNames(oldLcName).withMaxRecords(1);
		DescribeLaunchConfigurationsResult res = aas
				.describeLaunchConfigurations(req);
		List<LaunchConfiguration> lcs = res.getLaunchConfigurations();

		if (lcs == null || lcs.size() != 1) {
			return null;
		} else {
			LaunchConfiguration oldLc = lcs.get(0);
			String versionPart = oldLcName.substring(oldLcName
					.lastIndexOf("-v"));
			String currentVersionNumber = versionPart.replace("-v", "");
			String nextVersionNumber = String.valueOf(Integer
					.valueOf(currentVersionNumber) + 1);
			System.out.println("Next version number is: #" + nextVersionNumber);
			String newLcName = oldLcName.replace(currentVersionNumber,
					nextVersionNumber);

			CreateLaunchConfigurationRequest clcr = new CreateLaunchConfigurationRequest()
					.withAssociatePublicIpAddress(
							oldLc.getAssociatePublicIpAddress())
					.withBlockDeviceMappings(oldLc.getBlockDeviceMappings())
					.withEbsOptimized(oldLc.getEbsOptimized())
					.withIamInstanceProfile(oldLc.getIamInstanceProfile())
					.withImageId(newAMI)
					.withInstanceMonitoring(oldLc.getInstanceMonitoring())
					.withInstanceType(oldLc.getInstanceType())
					.withKeyName(oldLc.getKeyName())
					.withLaunchConfigurationName(newLcName)
					.withPlacementTenancy(oldLc.getPlacementTenancy())
					.withSecurityGroups(oldLc.getSecurityGroups())
					.withSpotPrice(oldLc.getSpotPrice())
					.withUserData(oldLc.getUserData());
			String kernelId = oldLc.getKernelId();
			if (kernelId != null && kernelId.length() != 0) {
				clcr.setKernelId(kernelId);
			}
			String ramdiskId = oldLc.getRamdiskId();
			if (ramdiskId != null && ramdiskId.length() != 0) {
				clcr.setRamdiskId(ramdiskId);
			}

			aas.createLaunchConfiguration(clcr);
			System.out.println(newLcName + " creation requested.");
			return newLcName;
		}
	}

	public String createNewLaunchConfigWithNewInstanceType(
			AmazonAutoScaling aas, String oldLcName, String newInstanceType) {
		DescribeLaunchConfigurationsRequest req = new DescribeLaunchConfigurationsRequest()
				.withLaunchConfigurationNames(oldLcName).withMaxRecords(1);
		DescribeLaunchConfigurationsResult res = aas
				.describeLaunchConfigurations(req);
		List<LaunchConfiguration> lcs = res.getLaunchConfigurations();

		if (lcs == null || lcs.size() != 1) {
			return null;
		} else {
			LaunchConfiguration oldLc = lcs.get(0);
			String versionPart = oldLcName.substring(oldLcName
					.lastIndexOf("-v"));
			String currentVersionNumber = versionPart.replace("-v", "");
			String nextVersionNumber = String.valueOf(Integer
					.valueOf(currentVersionNumber) + 1);
			System.out.println("Next version number is: #" + nextVersionNumber);
			String newLcName = oldLcName.replace(currentVersionNumber,
					nextVersionNumber);
			CreateLaunchConfigurationRequest clcr = new CreateLaunchConfigurationRequest()
					.withAssociatePublicIpAddress(
							oldLc.getAssociatePublicIpAddress())
					.withBlockDeviceMappings(oldLc.getBlockDeviceMappings())
					// .withEbsOptimized(oldLc.getEbsOptimized())
					.withIamInstanceProfile(oldLc.getIamInstanceProfile())
					.withImageId(oldLc.getImageId())
					.withInstanceMonitoring(oldLc.getInstanceMonitoring())
					.withInstanceType(newInstanceType)
					.withKeyName(oldLc.getKeyName())
					.withLaunchConfigurationName(newLcName)
					.withPlacementTenancy(oldLc.getPlacementTenancy())
					.withSecurityGroups(oldLc.getSecurityGroups())
					.withSpotPrice(oldLc.getSpotPrice())
					.withUserData(oldLc.getUserData());
			String kernelId = oldLc.getKernelId();
			if (kernelId != null && kernelId.length() != 0) {
				clcr.setKernelId(kernelId);
			}
			String ramdiskId = oldLc.getRamdiskId();
			if (ramdiskId != null && ramdiskId.length() != 0) {
				clcr.setRamdiskId(ramdiskId);
			}

			aas.createLaunchConfiguration(clcr);
			System.out.println(newLcName + " creation requested.");
			return newLcName;
		}
	}

	/**
	 * Change the AMI for existing ASG. Soft mode for rolling update.
	 * 
	 * @param aas
	 * @param asgName
	 * @param newAMI
	 * @param hard
	 * @param waitMins
	 *            In soft mode, you can specify the rolling termination rate.
	 */
	public void changeAMIForAsg(AmazonAutoScaling aas, String asgName,
			String newAMI, boolean hard, int waitMins) {
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		List<AutoScalingGroup> asgs = res.getAutoScalingGroups();
		if (asgs == null || asgs.size() != 1) {
			System.out.println("No such auto scaling group: " + asgName);
			System.exit(1);
		} else {
			AutoScalingGroup asg = asgs.get(0);
			String lcName = asg.getLaunchConfigurationName();
			if (lcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			String newLcName = this.createNewLaunchConfigWithNewAMI(aas,
					lcName, newAMI);
			if (newLcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			UpdateAutoScalingGroupRequest uasgr = new UpdateAutoScalingGroupRequest()
					.withAutoScalingGroupName(asgName)
					.withLaunchConfigurationName(newLcName);
			aas.updateAutoScalingGroup(uasgr);
			System.out.println(asgName + " change to use " + newLcName);
			if (hard) {
				System.out.println("hard mode.");
				terminateInstancesInAsgAtOnce(aas, asgName);
			} else {
				terminateInstancesInAsgRolling(aas, asgName, waitMins);
			}
			this.deleteObsoleteLaunchConfig(aas, newLcName);
		}
		System.out.println("Change AMI for ASG completed.");
	}

	public void changeInstanceTypeForAsg(AmazonAutoScaling aas, String asgName,
			String newInstanceType, boolean hard, int waitMins) {
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		List<AutoScalingGroup> asgs = res.getAutoScalingGroups();
		if (asgs == null || asgs.size() != 1) {
			System.out.println("No such auto scaling group: " + asgName);
			System.exit(1);
		} else {
			AutoScalingGroup asg = asgs.get(0);
			String lcName = asg.getLaunchConfigurationName();
			if (lcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			String newLcName = this.createNewLaunchConfigWithNewInstanceType(
					aas, lcName, newInstanceType);
			if (newLcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			UpdateAutoScalingGroupRequest uasgr = new UpdateAutoScalingGroupRequest()
					.withAutoScalingGroupName(asgName)
					.withLaunchConfigurationName(newLcName);
			aas.updateAutoScalingGroup(uasgr);
			System.out.println(asgName + " change to use " + newLcName);
			if (hard) {
				System.out.println("hard mode.");
				terminateInstancesInAsgAtOnce(aas, asgName);
			} else {
				terminateInstancesInAsgRolling(aas, asgName, waitMins);
			}
			this.deleteObsoleteLaunchConfig(aas, newLcName);
		}
		System.out.println("Change instance type for ASG completed.");
	}

	public String createNewLaunchConfigWithNewAMIAndUserdata(
			AmazonAutoScaling aas, String oldLcName, String newAMI,
			String userdataBase64) {

		DescribeLaunchConfigurationsRequest req = new DescribeLaunchConfigurationsRequest()
				.withLaunchConfigurationNames(oldLcName).withMaxRecords(1);
		DescribeLaunchConfigurationsResult res = aas
				.describeLaunchConfigurations(req);
		List<LaunchConfiguration> lcs = res.getLaunchConfigurations();

		if (lcs == null || lcs.size() != 1) {
			return null;
		} else {
			LaunchConfiguration oldLc = lcs.get(0);
			String versionPart = oldLcName.substring(oldLcName
					.lastIndexOf("-v"));
			String currentVersionNumber = versionPart.replace("-v", "");
			String nextVersionNumber = String.valueOf(Integer
					.valueOf(currentVersionNumber) + 1);
			System.out.println("Next version number is: #" + nextVersionNumber);
			String newLcName = oldLcName.replace(currentVersionNumber,
					nextVersionNumber);
			CreateLaunchConfigurationRequest clcr = new CreateLaunchConfigurationRequest()
					.withAssociatePublicIpAddress(
							oldLc.getAssociatePublicIpAddress())
					.withBlockDeviceMappings(oldLc.getBlockDeviceMappings())
					.withEbsOptimized(oldLc.getEbsOptimized())
					.withIamInstanceProfile(oldLc.getIamInstanceProfile())
					.withImageId(newAMI)
					.withInstanceMonitoring(oldLc.getInstanceMonitoring())
					.withInstanceType(oldLc.getInstanceType())
					.withKeyName(oldLc.getKeyName())
					.withLaunchConfigurationName(newLcName)
					.withPlacementTenancy(oldLc.getPlacementTenancy())
					.withSecurityGroups(oldLc.getSecurityGroups())
					.withSpotPrice(oldLc.getSpotPrice())
					.withUserData(userdataBase64);
			String kernelId = oldLc.getKernelId();
			if (kernelId != null && kernelId.length() != 0) {
				clcr.setKernelId(kernelId);
			}
			String ramdiskId = oldLc.getRamdiskId();
			if (ramdiskId != null && ramdiskId.length() != 0) {
				clcr.setRamdiskId(ramdiskId);
			}

			aas.createLaunchConfiguration(clcr);
			System.out.println(newLcName + " creation requested.");
			return newLcName;
		}
	}

	public void changeAMIAndUserdataForAsg(AmazonAutoScaling aas,
			String asgName, String newAMI, String userdataBase64, boolean hard,
			int waitMins) {
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		List<AutoScalingGroup> asgs = res.getAutoScalingGroups();
		if (asgs == null || asgs.size() != 1) {
			System.out.println("No such auto scaling group: " + asgName);
			System.exit(1);
		} else {
			AutoScalingGroup asg = asgs.get(0);
			String lcName = asg.getLaunchConfigurationName();
			String newLcName = this.createNewLaunchConfigWithNewAMIAndUserdata(
					aas, lcName, newAMI, userdataBase64);
			if (newLcName == null) {
				System.out.println("Error at finding LC: " + lcName);
				System.exit(1);
			}
			UpdateAutoScalingGroupRequest uasgr = new UpdateAutoScalingGroupRequest()
					.withAutoScalingGroupName(asgName)
					.withLaunchConfigurationName(newLcName);
			aas.updateAutoScalingGroup(uasgr);
			System.out.println(asgName + " change to use " + newLcName);
			if (hard) {
				terminateInstancesInAsgAtOnce(aas, asgName);
			} else {
				terminateInstancesInAsgRolling(aas, asgName, waitMins);
			}
			this.deleteObsoleteLaunchConfig(aas, newLcName);
		}
		System.out.println("Change AMI and userdata for ASG completed.");
	}
	
	public void changeLaunchConfigurationForAsg(AmazonAutoScaling aas, String asgName, String lcName, boolean hard, int waitMins){
		UpdateAutoScalingGroupRequest uasgr = new UpdateAutoScalingGroupRequest()
		.withAutoScalingGroupName(asgName)
		.withLaunchConfigurationName(lcName);
		aas.updateAutoScalingGroup(uasgr);
		System.out.println(asgName + " change to use " + lcName);
		if (hard) {
			System.out.println("hard mode.");
			terminateInstancesInAsgAtOnce(aas, asgName);
		} else {
			terminateInstancesInAsgRolling(aas, asgName, waitMins);
		}
	}
	
	public void printLaunchConfigurationForAsg(AmazonAutoScaling aas, String asgName){
		String currentLcName = aas.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asgName)).getAutoScalingGroups().get(0).getLaunchConfigurationName();
		System.out.println("______________\\");
		System.out.println("Current launch configuration: "+currentLcName);
		String lcNamePrefix = currentLcName.replaceAll("-v[0-9]+$", "");
		List<LaunchConfiguration> confs = aas.describeLaunchConfigurations().getLaunchConfigurations();
		ArrayList<String> candidateConfNames = new ArrayList<String>();
		String nameHolder = null;
		for(LaunchConfiguration lc:confs){
			nameHolder = lc.getLaunchConfigurationName();
			if(nameHolder.startsWith(lcNamePrefix)){
				candidateConfNames.add(nameHolder);
			}
		}
		System.out.println("______________\\");
		for(String s:candidateConfNames){
			System.out.println("Available launch configuration: "+s);
		}
	}
	
	public void swapLaunchConfigurationForAsg(AmazonAutoScaling aas, String asgName, boolean hard, int waitMins){
		String currentLcName = aas.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asgName)).getAutoScalingGroups().get(0).getLaunchConfigurationName();
		System.out.println("Current launch configuration: "+currentLcName);
		String lcNamePrefix = currentLcName.replaceAll("-v[0-9]+$", "");
		List<LaunchConfiguration> confs = aas.describeLaunchConfigurations().getLaunchConfigurations();
		ArrayList<String> candidateConfNames = new ArrayList<String>();
		String nameHolder = null;
		for(LaunchConfiguration lc:confs){
			nameHolder = lc.getLaunchConfigurationName();
			if(nameHolder.startsWith(lcNamePrefix)){
				candidateConfNames.add(nameHolder);
				if(candidateConfNames.size()>2){
					System.out.println("Candidate launch configurations are greater than two. Nothing to do.");
					return;
				}
			}
		}
		if(candidateConfNames.size()!=2){
			System.out.println("Candidate launch configurations are not enough. Nothing to do.");
			return;
		}
		System.out.println("Launch configurations available:");
		for(String s:candidateConfNames){
			System.out.println(s);
		}
		candidateConfNames.remove(currentLcName);
		this.changeLaunchConfigurationForAsg(aas, asgName, candidateConfNames.get(0), hard, waitMins);
	}
	

	/**
	 * This method suitable for ASG across two AZs.
	 * 
	 * @param aas
	 * @param asgName
	 * @param waitMins
	 */
	public void terminateInstancesInAsgRolling(AmazonAutoScaling aas,
			String asgName, int waitMins) {
		DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest()
				.withAutoScalingGroupNames(asgName).withMaxRecords(1);
		DescribeAutoScalingGroupsResult res = aas
				.describeAutoScalingGroups(req);
		// List<Instance> oldInstances =
		// res.getAutoScalingGroups().get(0).getInstances();
		Integer originalSize = res.getAutoScalingGroups().get(0)
				.getDesiredCapacity();
		Integer originalMaxSize = res.getAutoScalingGroups().get(0)
				.getMaxSize();
		int increment = 1;
		int optCount = originalSize;
		if (originalSize % 2 == 0) {
			increment = 2;
			optCount = originalSize / 2;
		}
		int switchedCount = 0;
		for (int i = 0; i < optCount; i++) {
			System.out.println("Swaping instance #" + increment + "...");
			// Add a new member.
			if ((originalSize + increment) > originalMaxSize) {
				aas.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
						.withAutoScalingGroupName(asgName)
						.withMaxSize(increment + originalSize)
						.withDesiredCapacity(increment + originalSize));
			} else {
				aas.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
						.withAutoScalingGroupName(asgName).withDesiredCapacity(
								increment + originalSize));
			}
			System.out.println("Growing group size to: "
					+ (increment + originalSize));
			try {
				// Cool down.
				System.out.println("Cool down...");
				Thread.sleep(1000 * 60 * waitMins);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Natural kill one old instance.
			System.out.println("Shrinking back to original group size: "
					+ originalSize);
			if (originalSize.equals(originalMaxSize)) {
				aas.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
						.withAutoScalingGroupName(asgName)
						.withDesiredCapacity(originalSize)
						.withMaxSize(originalMaxSize));
			} else {
				aas.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
						.withAutoScalingGroupName(asgName).withDesiredCapacity(
								originalSize));
			}
			switchedCount = switchedCount + increment;
			System.out.println((switchedCount) + " instance switched.");
		}
		System.out.println("Rolling terminate instances in ASG: " + asgName
				+ " completed.");
	}

}
