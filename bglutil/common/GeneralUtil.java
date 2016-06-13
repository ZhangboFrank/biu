package bglutil.common;

import java.io.UnsupportedEncodingException;
import java.util.TreeSet;

import sun.misc.BASE64Encoder;
import bglutil.Biu;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.config.AmazonConfig;
import com.amazonaws.services.config.AmazonConfigClient;
import com.amazonaws.services.datapipeline.DataPipeline;
import com.amazonaws.services.datapipeline.DataPipelineClient;
import com.amazonaws.services.directory.AWSDirectoryService;
import com.amazonaws.services.directory.AWSDirectoryServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class GeneralUtil {

	public static String base64Encode(String plain) {
		byte[] b = null;
		String base64 = null;
		try {
			b = plain.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (b != null) {
			base64 = new BASE64Encoder().encode(b);
		}
		return base64;
	}

	public static String versionNumberIncrease(String currentName) {
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

	public static void showAllGlobalResource() throws Exception {
		System.out.println("\n############## Global #############");
		AmazonIdentityManagement iam = (AmazonIdentityManagement) Clients
				.getClientByProfile(Clients.IAM, "virginia");
		IAMUtil util = new IAMUtil();
		util.printAllPhysicalId(iam);
	}

	private static void showCheckingSection(String section) {
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		System.out.println("# Checking " + section + "...");
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}

	public static void showAllResourceInProfile(String profile)
			throws Exception {
		Regions regions = Biu.PROFILE_REGIONS.get(profile);
		Region region = Region.getRegion(regions);
		System.out.println("\n############## " + region + " ("
				+ Biu.REGION_NAMES.get(region.getName())
				+ ") #############");
		TreeSet<String> ts = new TreeSet<String>(
				Biu.SERVICE_PACK_NAMES.keySet());
		for (String serviceName : ts) {
			if (region.isServiceSupported(serviceName)) {
				String service = Biu.SERVICE_PACK_NAMES.get(serviceName);
				Object client = Clients.getClientByProfile(service, profile);
				if (client instanceof AmazonKinesisClient) {
					KinesisUtil util = new KinesisUtil();
					showCheckingSection("Kinesis");
					util.printAllPhysicalId((AmazonKinesis) client);
				} else if (client instanceof AmazonGlacierClient) {
					GlacierUtil util = new GlacierUtil();
					showCheckingSection("Glacier");
					util.printAllPhysicalId((AmazonGlacier) client);
				} else if (client instanceof AmazonECSClient) {
					ECSUtil util = new ECSUtil();
					showCheckingSection("ECS");
					util.printAllPhysicalId((AmazonECS) client);
				} else if (client instanceof DataPipelineClient) {
					DataPipelineUtil util = new DataPipelineUtil();
					showCheckingSection("DataPipeline");
					util.printAllPhysicalId((DataPipeline) client);
				} else if (client instanceof AmazonCodeDeployClient) {
					CodeDeployUtil util = new CodeDeployUtil();
					showCheckingSection("CodeDeploy");
					util.printAllPhysicalId((AmazonCodeDeploy) client);
				} else if (client instanceof AWSLambdaClient) {
					LambdaUtil util = new LambdaUtil();
					showCheckingSection("Lambda");
					util.printAllPhysicalId((AWSLambda) client);
				} else if (client instanceof AmazonCloudFormationClient) {
					CFNUtil util = new CFNUtil();
					showCheckingSection("CloudFormation");
					util.printAllPhysicalId((AmazonCloudFormation) client);
				} else if (client instanceof AmazonRDSClient) {
					RDSUtil util = new RDSUtil();
					showCheckingSection("RDS");
					util.printAllPhysicalId((AmazonRDS) client);
				} else if (client instanceof AmazonSQSClient) {
					SQSUtil util = new SQSUtil();
					showCheckingSection("SQS");
					util.printAllPhysicalId((AmazonSQS) client);
				} else if (client instanceof AmazonCloudWatchClient) {
					CWUtil util = new CWUtil();
					showCheckingSection("CloudWatch");
					util.printAllPhysicalId((AmazonCloudWatch) client);
				} else if (client instanceof AmazonSNSClient) {
					SNSUtil util = new SNSUtil();
					showCheckingSection("SNS");
					util.printAllPhysicalId((AmazonSNS) client);
				} else if (client instanceof AmazonDynamoDBClient) {
					DynamoDBUtil util = new DynamoDBUtil();
					showCheckingSection("DynamoDB");
					util.printAllPhysicalId((AmazonDynamoDB) client);
				} else if (client instanceof AmazonElastiCacheClient) {
					ElastiCacheUtil util = new ElastiCacheUtil();
					showCheckingSection("ElastiCache");
					util.printAllPhysicalId((AmazonElastiCache) client);
				} else if (client instanceof AmazonElasticLoadBalancingClient) {
					ELBUtil util = new ELBUtil();
					showCheckingSection("ELB");
					util.printAllPhysicalId((AmazonElasticLoadBalancing) client);
				} else if (client instanceof AmazonAutoScalingClient) {
					ASGUtil util = new ASGUtil();
					showCheckingSection("ASG");
					util.printAllPhysicalId((AmazonAutoScaling) client);
				} else if (client instanceof AmazonEC2Client) {
					EC2Util util = new EC2Util();
					showCheckingSection("EC2");
					util.printAllPhysicalId((AmazonEC2) client);
				} else if (client instanceof AmazonElasticMapReduceClient) {
					EMRUtil util = new EMRUtil();
					showCheckingSection("EMR");
					util.printAllPhysicalId((AmazonElasticMapReduce) client);
				} else if (client instanceof AWSKMSClient) {
					KMSUtil util = new KMSUtil();
					showCheckingSection("KMS");
					util.printAllPhysicalId((AWSKMS) client);
				} else if (client instanceof AmazonConfigClient) {
					ConfigUtil util = new ConfigUtil();
					showCheckingSection("Config");
					util.printAllPhysicalId((AmazonConfig) client);
				} else if (client instanceof AWSDirectoryServiceClient) {
					DirectoryUtil util = new DirectoryUtil();
					showCheckingSection("Directory");
					util.printAllPhysicalId((AWSDirectoryService) client);
				} else if (client instanceof AWSLogsClient) {
					LogsUtil util = new LogsUtil();
					showCheckingSection("CloudWatch Logs");
					util.printAllPhysicalId((AWSLogsClient) client);
				} else if (client instanceof AmazonIdentityManagementClient) {
					IAMUtil util = new IAMUtil();
					showCheckingSection("IAM");
					util.printAllPhysicalId((AmazonIdentityManagementClient) client);
				}
				/*
				 * Always generate timeout exception. else if(client instanceof
				 * AmazonWorkspacesClient){ WorkspacesUtil util = new
				 * WorkspacesUtil();
				 * System.out.println("> Checking Workspaces resources...");
				 * util.printAllPhysicalId((AmazonWorkspaces) client); }
				 */
			}
		}
	}

	public static void showAllResource() throws Exception {
		for (Regions regions : Biu.ALL_REGIONS) {
			showAllResourceInProfile(regions.getName());
		}
		showAllGlobalResource();
	}
}
