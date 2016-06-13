package bglutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import bglutil.common.ASGUtil;
import bglutil.common.CFNUtil;
import bglutil.common.Clients;
import bglutil.common.DynamoDBUtil;
import bglutil.common.EC2Util;
import bglutil.common.ELBUtil;
import bglutil.common.EMRUtil;
import bglutil.common.ElastiCacheUtil;
import bglutil.common.GeneralUtil;
import bglutil.common.GlacierUtil;
import bglutil.common.Helper;
import bglutil.common.IAMUtil;
import bglutil.common.KMSUtil;
import bglutil.common.KinesisUtil;
import bglutil.common.S3Util;
import bglutil.common.STSUtil;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.ServiceAbbreviations;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.model.ClusterSummary;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;

/**
 * Biu!!!
 * @author guanglei
 */
public class Biu {
	
	private static final Helper h = new Helper();
	
	private static final String CONFIG_BACKUP_BUCKET = ""; // Used for routines require a bucket for staging.
	
	private static final String CONFIG_APP_NACL_BEIJING = ""; // Used for close/openBeijingNetwork()
	private static final String CONFIG_DEFAULT_NACL_BEIJING = ""; // Used for close/openBeijingNetwork()
	private static final String CONFIG_APP_NACL_VIR = ""; // Used for close/openVirNetwork()
	private static final String CONFIG_APP_NACL_TOKYO = ""; // Used for close/openTokyoNetwork()
	private static final String CONFIG_DEFAULT_NACL_TOKYO = ""; // Used for close/openTokyoNetwork()
	private static final String CONFIG_MFA_USERNAME = ""; // Used for MFA demo
	private static final String CONFIG_MFA_ARN = ""; // Used for MFA demo
	private static final String CONFIG_EXAMPLE_IAM_ROLE_ARN = ""; // Used for AssumeRole demo
	private static final String CONFIG_BEIJING_EC2_KEYPAIR_NAME = ""; // Used for EC2 and ASG demo
	private static final String CONFIG_BEIJING_DEFAULT_VPC_SUBNET1 = ""; // Used for EC2 and ASG demo
	private static final String CONFIG_BEIJING_DEFAULT_VPC_SUBNET2 = ""; // Used for EC2 and ASG demo
	private static final String CONFIG_BEIJING_DEFAULT_VPC_ALLOWALL_SECURITY_GROUP = ""; // Used for EC2 and ASG demo
	
	public static final HashMap<String,String> SERVICE_PACK_NAMES = new HashMap<String,String>();
	public static final HashMap<String,String> REGION_NAMES = new HashMap<String,String>();
	public static final HashMap<String,Regions> PROFILE_REGIONS = new HashMap<String,Regions>();

	static {
		PROFILE_REGIONS.put("default", Regions.CN_NORTH_1);
		PROFILE_REGIONS.put("global", Regions.US_EAST_1);
		PROFILE_REGIONS.put("china", Regions.CN_NORTH_1);
		PROFILE_REGIONS.put(CONFIG_MFA_USERNAME, Regions.US_EAST_1);
		
		PROFILE_REGIONS.put("beijing", Regions.CN_NORTH_1);
		PROFILE_REGIONS.put("virginia", Regions.US_EAST_1);
		PROFILE_REGIONS.put("tokyo", Regions.AP_NORTHEAST_1);
		PROFILE_REGIONS.put("seoul", Regions.AP_NORTHEAST_2);
		PROFILE_REGIONS.put("singapore", Regions.AP_SOUTHEAST_1);
		PROFILE_REGIONS.put("oregon", Regions.US_WEST_2);
		PROFILE_REGIONS.put("california", Regions.US_WEST_1);
		PROFILE_REGIONS.put("ireland", Regions.EU_WEST_1);
		PROFILE_REGIONS.put("frankfurt", Regions.EU_CENTRAL_1);
		PROFILE_REGIONS.put("sydney", Regions.AP_SOUTHEAST_2);
		PROFILE_REGIONS.put("sanpaulo", Regions.SA_EAST_1);
		
		REGION_NAMES.put("cn-north-1", "Beijing");
		REGION_NAMES.put("us-east-1", "N. Virginia");
		REGION_NAMES.put("us-west-2", "Oregon");
		REGION_NAMES.put("us-west-1", "N. California");
		REGION_NAMES.put("eu-west-1", "Ireland");
		REGION_NAMES.put("eu-central-1", "Frankfurt");
		REGION_NAMES.put("ap-southeast-1", "Singapore");
		REGION_NAMES.put("ap-northeast-1", "Tokyo");
		REGION_NAMES.put("ap-northeast-2", "Seoul");
		REGION_NAMES.put("ap-southeast-2", "Sydney");
		REGION_NAMES.put("sa-east-1", "São Paulo");
		
		SERVICE_PACK_NAMES.put("sts", Clients.STS);
		SERVICE_PACK_NAMES.put("s3", Clients.S3);
		SERVICE_PACK_NAMES.put("dynamodb", Clients.DDB);
		SERVICE_PACK_NAMES.put("iam", Clients.IAM);
		SERVICE_PACK_NAMES.put("cloudfront", Clients.CF);
		SERVICE_PACK_NAMES.put("rds", Clients.RDS);
		SERVICE_PACK_NAMES.put("sns", Clients.SNS);
		SERVICE_PACK_NAMES.put("sqs", Clients.SQS);
		SERVICE_PACK_NAMES.put("elasticache", Clients.ELASTICACHE);
		SERVICE_PACK_NAMES.put("cloudformation", Clients.CFN);
		SERVICE_PACK_NAMES.put("glacier", Clients.GLACIER);
		SERVICE_PACK_NAMES.put("elasticmapreduce", Clients.EMR);
		SERVICE_PACK_NAMES.put("lambda", Clients.LAMBDA);
		SERVICE_PACK_NAMES.put("ecs", Clients.ECS);
		SERVICE_PACK_NAMES.put("storagegateway", Clients.SGW);
		SERVICE_PACK_NAMES.put("elasticfilesystem", Clients.EFS);
		SERVICE_PACK_NAMES.put("redshift", Clients.REDSHIFT);
		SERVICE_PACK_NAMES.put("directconnect", Clients.DX);
		SERVICE_PACK_NAMES.put("route53", Clients.R53);
		SERVICE_PACK_NAMES.put("monitoring", Clients.CW);
		SERVICE_PACK_NAMES.put("logs", Clients.LOGS);
		SERVICE_PACK_NAMES.put("directory", Clients.DS);
		SERVICE_PACK_NAMES.put("support", Clients.SUPPORT);
		SERVICE_PACK_NAMES.put("cloudtrail", Clients.CLOUDTRAIL);
		SERVICE_PACK_NAMES.put("config", Clients.CONFIG);
		SERVICE_PACK_NAMES.put("elasticbeanstalk", Clients.EB);
		SERVICE_PACK_NAMES.put("opsworks", Clients.OPSWORKS);
		SERVICE_PACK_NAMES.put("codedeploy", Clients.CODEDEPLOY);
		SERVICE_PACK_NAMES.put("datapipeline", Clients.DATAPIPELINE);
		SERVICE_PACK_NAMES.put("machinelearning", Clients.ML);
		SERVICE_PACK_NAMES.put("swf", Clients.SWF);
		SERVICE_PACK_NAMES.put("elastictranscoder", Clients.TRANSCODER);
		SERVICE_PACK_NAMES.put("email", Clients.SES);
		SERVICE_PACK_NAMES.put("cloudsearch", Clients.CLOUDSEARCH);
		SERVICE_PACK_NAMES.put("cognito-identity", Clients.COGNITOID);
		SERVICE_PACK_NAMES.put("cognito-sync", Clients.COGNITOSYNC);
		SERVICE_PACK_NAMES.put("workspaces", Clients.WORKSPACES);
		SERVICE_PACK_NAMES.put("autoscaling", Clients.ASG);
		SERVICE_PACK_NAMES.put("elasticloadbalancing", Clients.ELB);
		SERVICE_PACK_NAMES.put("ec2", Clients.EC2);
		SERVICE_PACK_NAMES.put("kms", Clients.KMS);
	}
	
	public static final String[] ALL_PROFILES = new String[]{"beijing","virginia","tokyo","seoul","singapore","oregon","california","ireland","frankfurt","sydney","sanpaulo"};
	public static final Regions[] GLOBAL_REGIONS = new Regions[]{Regions.US_EAST_1,Regions.AP_NORTHEAST_1,Regions.AP_SOUTHEAST_1,Regions.AP_SOUTHEAST_2,Regions.SA_EAST_1,Regions.US_WEST_1,Regions.US_WEST_2,Regions.EU_WEST_1, Regions.EU_CENTRAL_1, Regions.AP_NORTHEAST_2};
	public static final Regions[] CHINA_REGIONS = new Regions[]{Regions.CN_NORTH_1};
	public static final Regions[] ALL_REGIONS = new Regions[]{Regions.CN_NORTH_1,Regions.US_EAST_1,Regions.AP_NORTHEAST_1,Regions.AP_SOUTHEAST_1,Regions.AP_SOUTHEAST_2,Regions.SA_EAST_1,Regions.US_WEST_1,Regions.US_WEST_2,Regions.EU_WEST_1, Regions.EU_CENTRAL_1, Regions.AP_NORTHEAST_2};
	
	// Oregon, Singapore, for mass scope testing, for example: dropmeAsYourWish series.
	public static final Regions[] TEST_REGIONS = new Regions[]{Regions.US_WEST_2,Regions.AP_SOUTHEAST_1}; 
	
	static {
		java.security.Security.setProperty("networkaddress.cache.ttl", "30");
	}
	
	/**
	 * Endless connect to specified address with or without http:// URI prefix.
	 * @param address
	 * @throws Exception
	 */
	public void urlTest(String address) throws Exception{
		h.help(address,"<url-for-testing>");
		if(!address.startsWith("http")){
			address="http://"+address;
		}
		ArrayList<URL> urls = new ArrayList<URL>();
		try {
			urls.add(new URL(address));
		} catch (MalformedURLException e) {
			// Will NOT happen.
			e.printStackTrace();
		}
		// Wait for port open.
		System.out.println(urls.get(0).toString());
		BufferedReader br;
		boolean dive = true;
		int count = 0;
		long start = 0L;
		long end = 0L;
		while (dive) {
			try {
				start = System.currentTimeMillis();
				br = new BufferedReader(new InputStreamReader(urls
						.get(0).openConnection().getInputStream()));
				String lineOne = br.readLine();
				end = System.currentTimeMillis();
				System.out.println(++count+": Reading first line: "+lineOne+" ("+(end-start)+"ms) OK.");
				br.close();
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				try {
					Thread.sleep(7 * 1000);
				} catch (InterruptedException e) {
				}
			} finally{
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	/**
	 * Show all regions' names.
	 */
	public static void showRegionName(){
		System.out.println();
		for(String r: Biu.REGION_NAMES.keySet()){
			System.out.println(r+" <==> "+REGION_NAMES.get(r));
		}
		System.out.println();
	}
	
	
	public void generateSsoUrlGlobal(String federatedUser) throws Exception{
		h.help(federatedUser,"<federatedUser>");
		STSUtil util = new STSUtil();
		AWSSecurityTokenService sts = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, "global");
		String url = util.getFederatedUserAwsConsoleSsoUrlForEc2S3AdminGlobal(sts, federatedUser, "http://aws.amazon.com");
		System.out.println(url);
	}
	
	public void generateSsoUrlChina(String federatedUser) throws Exception{
		h.help(federatedUser,"<federatedUser>");
		STSUtil util = new STSUtil();
		AWSSecurityTokenService sts = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, "china");
		String url = util.getFederatedUserAwsConsoleSsoUrlForEc2S3AdminChina(sts, federatedUser, "http://www.amazonaws.cn");
		System.out.println(url);
	}
	
	// TODO
	public void purgeVault(String vaultName, String profile) throws Exception{
		h.help(vaultName,"<vault-name> <profile>");
		GlacierUtil util = new GlacierUtil();
		AmazonGlacier g = (AmazonGlacier) Clients.getClientByProfile(Clients.GLACIER, profile);
		util.purgeVault(g, vaultName);
	}
	
	public void clearOrphanSnapshot(String profile) throws Exception{
		h.help(profile,"<profile>");
		EC2Util util = new EC2Util();
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		AWSSecurityTokenService sts = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, profile);
		util.clearOrphanSnapshot(ec2, sts.getCallerIdentity(new GetCallerIdentityRequest()).getAccount());
	}
	
	/**
	 * Show instance health status for an ELB.
	 * @param elbName
	 * @param profile
	 * @throws Exception
	 */
	public void showInstanceHealthByELb(String elbName, String profile) throws Exception{
		h.help(elbName,"<elb-name> <profile>");
		ELBUtil util = new ELBUtil();
		AmazonElasticLoadBalancing elb = (AmazonElasticLoadBalancing) Clients.getClientByProfile(Clients.ELB, profile);
		util.describeInstanceHealthByElbName(elb, elbName);
	}
	
	/**
	 * Copy source bucket to target bucket within ideology region.
	 * @param regionPartition
	 * @param sourceBucketName
	 * @param destinationBucketName
	 * @throws Exception
	 */
	public void copyBucket(String regionPartition, String sourceBucketName, String destinationBucketName) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <source-bucket> <destination-bucket>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.copyBucket(s3, sourceBucketName, destinationBucketName);
	}
	
	/**
	 * Generate/Encrypt/Decrypt data key.
	 * @param keyId
	 * @param profile
	 * @throws Exception
	 */
	public void demoKms(String keyId, String profile) throws Exception{
		h.help(keyId,"<key-id> <profile>");
		KMSUtil util = new KMSUtil();
		AWSKMSClient kms = (AWSKMSClient) Clients.getClientByProfile(Clients.KMS, profile);
		util.generateDataKeyAndDecrypt(kms, keyId);
	}
	
	public void showCaller(String profile) throws Exception{
		h.help(profile,"<profile>");
		STSUtil util = new STSUtil();
		AWSSecurityTokenService sts = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, profile);
		GetCallerIdentityResult result = util.getCallerId(sts);
		System.out.println("Account:\t"+result.getAccount());
		System.out.println("User:\t\t"+result.getUserId());
		System.out.println("ARN:\t\t"+result.getArn());
	}
	
	public void demoStsFederation(String username) throws Exception{
		h.help(username,"<username>");
		STSUtil util = new STSUtil();
		AWSSecurityTokenService stsChina = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, "china");
		Policy policy = new Policy()
		.withStatements(
				new Statement(Effect.Allow)
				.withActions(S3Actions.GetObject)
				.withResources(new Resource("*")));
		System.out.println("Calling user: "+stsChina.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
		System.out.println("Setting policy: ");
		System.out.println(policy.toJson());
		BasicSessionCredentials bsc3 = null;
		try{
			System.out.println("Calling GetFederationToken...");
			bsc3 = util.getFederatedUserToken(stsChina, 900, username, policy);
			System.out.println("Temp Access Key Id: "+bsc3.getAWSAccessKeyId());
			System.out.println("Temp Secret Key: "+bsc3.getAWSSecretKey());
			System.out.print("Temp Session Token: "+bsc3.getSessionToken());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
	}
	
	public void demoSts(String tokenCode) throws Exception{
		h.help(tokenCode,"<token-code-of-mfa>");
		int durationSec = 900;
		if(tokenCode.equals("------")){
			durationSec=999999999;
		}
		STSUtil util = new STSUtil();
		
		h.title("GetSessionToken /w MFA from Permanent Crendentials");
		AWSSecurityTokenService stsPawnGlobal = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, CONFIG_MFA_USERNAME);
		try{
			System.out.println("Calling user: "+stsPawnGlobal.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
			BasicSessionCredentials bsc1 = util.getSessionTokenMFA(stsPawnGlobal, durationSec, CONFIG_MFA_ARN, tokenCode);
			System.out.println("Temp AK: "+bsc1.getAWSAccessKeyId());
		}catch (AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		AWSSecurityTokenService stsChina = (AWSSecurityTokenService) Clients.getClientByProfile(Clients.STS, "beijing");
		h.title("GetSessionToken from Permanent Credentials");
		System.out.println("Calling user: "+stsChina.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
		try{
			BasicSessionCredentials bsc2 = util.getSessionToken(stsChina, durationSec);
			System.out.println("Temp AK: "+bsc2.getAWSAccessKeyId());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("GetFederatedUserToken from Permanent Credentials");
		Policy policy = new Policy()
						.withStatements(
								new Statement(Effect.Allow)
								.withActions(S3Actions.GetObject)
								.withResources(new Resource("*")));
		System.out.println("Calling user: "+stsChina.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
		BasicSessionCredentials bsc3 = null;
		try{
			bsc3 = util.getFederatedUserToken(stsChina, durationSec, "batman007", policy);
			System.out.println("Temp AK: "+bsc3.getAWSAccessKeyId());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("AssumeRole from Permanent Credentials");
		//String assumedUserArn = null;
		System.out.println("Calling user: "+stsChina.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
		BasicSessionCredentials bsc4 = null;
		try{
			bsc4 = util.assumeRole(stsChina, durationSec, CONFIG_EXAMPLE_IAM_ROLE_ARN, "developer");
			System.out.println("Temp AK: "+bsc4.getAWSAccessKeyId());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("AssumeRole after AssumeRole");
		AWSSecurityTokenService stsRole = new AWSSecurityTokenServiceClient(bsc4);
		stsRole.setRegion(Region.getRegion(Regions.CN_NORTH_1));
		try{
			System.out.println("Calling user: "+stsRole.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
			BasicSessionCredentials bsc14 = util.assumeRole(stsChina, durationSec, CONFIG_EXAMPLE_IAM_ROLE_ARN, "developer");
			System.out.println("AK: "+bsc14.getAWSAccessKeyId());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("GetFederatedUserToken after AssumeRole");
		AWSSecurityTokenService stsTemp = new AWSSecurityTokenServiceClient(bsc4);
		stsTemp.setRegion(Region.getRegion(Regions.CN_NORTH_1));
		try{
			System.out.println("Calling user: "+stsTemp.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
			BasicSessionCredentials bsc5 = util.getFederatedUserToken(stsTemp, durationSec, "batman007", new Policy()
																								.withStatements(
																									new Statement(Effect.Allow)
																									.withActions(S3Actions.GetObject)
																									.withResources(new Resource("*"))));
			System.out.println("AK: "+bsc5.getAWSAccessKeyId());
		}catch(AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("AssumeRole after GetFederatedUserToken");
		AWSSecurityTokenService stsFed = new AWSSecurityTokenServiceClient(bsc3);
		stsFed.setRegion(Region.getRegion(Regions.CN_NORTH_1));
		System.out.println("Calling user: "+stsFed.getCallerIdentity(new GetCallerIdentityRequest()).getArn());
		try{
			BasicSessionCredentials bsc10 = util.assumeRole(stsFed, durationSec, CONFIG_EXAMPLE_IAM_ROLE_ARN, "batman007");
			System.out.println("Temp AK: "+bsc10.getAWSAccessKeyId());
		}catch (AmazonServiceException ex){
			System.out.println(ex.getMessage());
		}
		
		
		AWSSecurityTokenService stsIp = new AWSSecurityTokenServiceClient(new InstanceProfileCredentialsProvider());
		stsIp.setRegion(Region.getRegion(Regions.CN_NORTH_1));
		h.title("GetSessionToken from Instance Profile");
		System.out.println("Calling user: EC2 Instance Profile");
		try{
			BasicSessionCredentials bsc6 = util.getSessionToken(stsIp, 900);
			System.out.println("Temp AK: "+bsc6.getAWSAccessKeyId());
		}catch(AmazonClientException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("GetFederatedUserToken from Instance Profile");
		System.out.println("Calling user: EC2 Instance Profile");
		Policy policyx = new Policy()
		.withStatements(
				new Statement(Effect.Allow)
				.withActions(S3Actions.GetObject)
				.withResources(new Resource("*")));
		try{
			BasicSessionCredentials bsc7 = util.getFederatedUserToken(stsIp, 900, "batman", policyx);
			System.out.println("Temp AK: "+bsc7.getAWSAccessKeyId());
		}catch(AmazonClientException ex){
			System.out.println(ex.getMessage());
		}
		
		h.title("AssumeRole from Instance Profile");
		System.out.println("Calling user: EC2 Instance Profile");
		try{
			BasicSessionCredentials bsc8 = util.assumeRole(stsIp, 900, CONFIG_EXAMPLE_IAM_ROLE_ARN, "one-ec2-instance");
			System.out.println("Temp AK: "+bsc8.getAWSAccessKeyId());
		}catch(AmazonClientException ex){
			System.out.println(ex.getMessage());
		}
	}
	
	public void showResource() throws Exception{
		GeneralUtil.showAllResource();
	}
	
	public void showResourceByProfile(String profile) throws Exception{
		h.help(profile,"<profile>");
		GeneralUtil.showAllResourceInProfile(profile);
	}
	
	public void kinesisProduceRandomRecords(String streamName, String dop, String recordsPerPut, String profile){
		h.help(streamName,"<stream-name> <dop> <records-per-put> <profile>");
		KinesisUtil util = new KinesisUtil();
		util.produceRandomRecords(streamName, Integer.parseInt(dop), Integer.parseInt(recordsPerPut), profile);
	}
	
	public void kinesisConsumeRandomRecords(String streamName, String initialPositionInStream, String profile) throws Exception{
		h.help(streamName,"<stream-name> <initial-position-in-stream: latest|trim_horizon> <profile>");
		KinesisUtil util = new KinesisUtil();
		InitialPositionInStream ipis = null;
		if(initialPositionInStream.equals("latest")){
			ipis = InitialPositionInStream.LATEST;
		}
		else if (initialPositionInStream.equals("trim_horizon")){
			ipis = InitialPositionInStream.TRIM_HORIZON;
		}
		else{
			System.out.println("Invalid InitialPositionInStream, available values: latest | trim_horizon.");
			return;
		}
		util.consumeRandomRecordsFromKinesisKCL(streamName, ipis, profile);
	}
	
	public void showServiceEndpoint(String regionName) throws Exception{
		h.help(regionName,"<region-name>");
		Region region = Region.getRegion(Regions.fromName(regionName));
		TreeSet<String> ts = new TreeSet<String>(Biu.SERVICE_PACK_NAMES.keySet());
		String endpoint = null;
		int i=1;
		for(String serviceName:ts){
			endpoint = region.getServiceEndpoint(serviceName);
			if(endpoint!=null && !endpoint.equals("null")){
				System.out.println(i+++") "+serviceName+":             \t"+endpoint);
			}
		}
	}
	
	/**
	 * Regionally drop all resources with a dropme prefix.
	 * A prefix is the value used in tag "Name", or the resource name if tagging is NOT available.
	 */
	public void troll(String objectPrefixToClean, String profile) throws Exception{
		h.help(objectPrefixToClean,"<object-prefix-to-clean> <profile>");
		if(!objectPrefixToClean.startsWith("dropme") && !objectPrefixToClean.startsWith("launch-wizard")){
				System.out.println("<object-prefix> must start with dropme -or- launch-wizard");
				return;
		}
		this.dropmeCfn(objectPrefixToClean, profile);
		this.dropmeAsg(objectPrefixToClean, profile); 
		this.dropmeLc(objectPrefixToClean, profile);
		this.dropmeEc2(objectPrefixToClean, profile);
		this.dropmeKeyPair(objectPrefixToClean, profile);
		this.dropmeElb(objectPrefixToClean, profile);
		this.dropmeElastiCache(objectPrefixToClean, profile);
		this.dropmeSg(objectPrefixToClean, profile);
		this.dropmeSns(objectPrefixToClean, profile);
		this.dropmeSqs(objectPrefixToClean, profile);
		this.dropmeSnapshot(objectPrefixToClean, profile);
		this.dropmeEbs(objectPrefixToClean, profile);
		this.dropmeVpc(objectPrefixToClean, profile);
		this.dropmeIamResource(objectPrefixToClean,profile);
		this.dropmeAmi(objectPrefixToClean,profile);
		this.dropmeEmr(objectPrefixToClean,profile);
		this.dropmeRt(objectPrefixToClean,profile);
	}
	
	public void dropmeRt(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop Route Table with name prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		Filter f = new Filter().withName("tag:Name").withValues(prefix+"*");
		for(RouteTable rt:ec2.describeRouteTables(new DescribeRouteTablesRequest().withFilters(f)).getRouteTables()){
			System.out.println("=> Deleting route table: "+rt.getRouteTableId());
			ec2.deleteRouteTable(new DeleteRouteTableRequest().withRouteTableId(rt.getRouteTableId()));
		}
	}
	
	public void dropmeEmr(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop EMR with name prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonElasticMapReduce emr = (AmazonElasticMapReduce) Clients.getClientByProfile(Clients.EMR, profile);
		EMRUtil util = new EMRUtil();
		List<ClusterSummary> list = util.getAliveEmrClusters(emr);
		for(ClusterSummary cs:list){
			if(cs.getName().startsWith(prefix)){
				util.terminateEmrCluster(emr, cs.getId());
			}
		}
	}
	
	public void dropmeAmi(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop AMI with name prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		for(Image i:ec2.describeImages(new DescribeImagesRequest().withOwners("self")).getImages()){
			if(i.getName().startsWith(prefix)){
				util.deregisterAmi(ec2, i.getImageId());
			}
		}
	}
	
	public void dropmeCfn(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop CFN stack with name prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		CFNUtil util = new CFNUtil();
		AmazonCloudFormation cfn = (AmazonCloudFormation) Clients.getClientByProfile(Clients.CFN, profile);
		ArrayList<String> targetStack = new ArrayList<String>();
		for(Stack s: cfn.describeStacks().getStacks()){
			if(s.getStackName().startsWith(prefix)){
				targetStack.add(s.getStackName());
				util.deleteCNFStack(cfn, s.getStackName());
			}
		}
		boolean test = true;
		int tests = 24;
		List<Stack> stacks = null;
		while(targetStack.size()>0 && test){
			tests--;
			if(tests==0){System.out.println("Timed out, please check the status manually"); break;}
			Thread.sleep(5000);
			for(String stackName:targetStack){
				stacks = cfn.describeStacks(new DescribeStacksRequest().withStackName(stackName)).getStacks();
				if(stacks!=null && stacks.size()>0){
					test = true;
					System.out.println("Waiting for deleting "+stackName);
					break;
				}
				test = false;
			}
		}
		if(!test){
			for(String stackName: targetStack){
				System.out.println("Stack "+stackName+" deleted");
			}
		}
	}
	
	public void dropmeKeyPair(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop Keypair with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		EC2Util util = new EC2Util();
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		List<KeyPairInfo> keys = ec2.describeKeyPairs().getKeyPairs();
		for(KeyPairInfo key:keys){
			if (key.getKeyName().startsWith(prefix)){
				util.dropKeypair(ec2, key.getKeyName());
			}
		}
	}
	
	public void dropmeLc(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop LC with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling asg = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		List<LaunchConfiguration> lcs = asg.describeLaunchConfigurations().getLaunchConfigurations();
		for(LaunchConfiguration lc:lcs){
			if(lc.getLaunchConfigurationName().startsWith(prefix)){
				util.deleteLcByName(asg, lc.getLaunchConfigurationName());
			}
		}
	}
	
	public void dropmeIamResource(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop IAM resources with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		IAMUtil util = new IAMUtil();
		AmazonIdentityManagement iam = (AmazonIdentityManagement) Clients.getClientByProfile(Clients.IAM, profile);
		for(Role role:iam.listRoles().getRoles()){
			if(role.getRoleName().startsWith(prefix)){
				System.out.println("=> Dropping Role: "+role.getRoleName());
				util.dropRole(iam, role.getRoleName());
			}
		}
		for(InstanceProfile ip:iam.listInstanceProfiles().getInstanceProfiles()){
			if(ip.getInstanceProfileName().startsWith(prefix)){
				System.out.println("=> Dropping Instance Profile: "+ip.getInstanceProfileName());
				util.dropInstanceProfile(iam, ip.getInstanceProfileName());
			}
		}
		for(User user:iam.listUsers().getUsers()){
			if(user.getUserName().startsWith(prefix)){
				System.out.println("=> Dropping User: "+user.getUserName());
				util.dropUser(iam, user.getUserName());
			}
		}
		for(Group group:iam.listGroups().getGroups()){
			if(group.getGroupName().startsWith(prefix)){
				System.out.println("=> Dropping Group: "+group.getGroupName());
				util.dropGroup(iam, group.getGroupName());
			}
		}
	}
	
	/**
	 * Drop ASG by ASG name prefix.
	 * @param prefix
	 * @param profile
	 * @throws Exception
	 */
	public void dropmeAsg(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop Auto Scaling Group with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		ASGUtil util = new ASGUtil();
				AmazonAutoScaling asg = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
				try{
					for(AutoScalingGroup group:asg.describeAutoScalingGroups().getAutoScalingGroups()){
						if(group.getAutoScalingGroupName().startsWith(prefix)){
							util.deleteAsgByName(asg, group.getAutoScalingGroupName());
						}
					}
				}
				catch(AmazonServiceException ex){
					System.out.println(ex.getMessage());
				}
	}
	
	/**
	 * Drop ElastiCache by ElastiCache cluster id prefix.
	 * @param prefix
	 * @param profile
	 * @throws Exception
	 */
	public void dropmeElastiCache(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop ElastiCache Cluster with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		ElastiCacheUtil util = new ElastiCacheUtil();
		AmazonElastiCache cache = (AmazonElastiCache) Clients.getClientByProfile(Clients.ELASTICACHE, profile);
		List<CacheCluster> clusters = cache.describeCacheClusters().getCacheClusters();
		for(CacheCluster cc:clusters){
			if (cc.getCacheClusterId().startsWith(prefix)){
				util.dropCacheClusterById(cache, cc.getCacheClusterId());
			}
		}
	}
	
	public void dropmeSqs(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop SQS with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonSQS sqs = (AmazonSQS) Clients.getClientByProfile(Clients.SQS, profile);
		for(String url:sqs.listQueues().getQueueUrls()){
			if(url.matches(".*/"+prefix+"[^/]*")){
				System.out.println("=> Dropping queue "+url);
				sqs.deleteQueue(new DeleteQueueRequest()
										.withQueueUrl(url));
			}
		}
	}
	
	public void dropmeSns(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop SNS with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonSNS sns = (AmazonSNS) Clients.getClientByProfile(Clients.SNS, profile);
		//SNSUtil util = new SNSUtil();
		List<Topic> topics = sns.listTopics().getTopics();
		for(Topic t:topics){
			if(t.getTopicArn().matches(".*:"+prefix+"[^:]*")){
				System.out.println("=> Dropping topic "+t.getTopicArn());
				sns.deleteTopic(new DeleteTopicRequest().withTopicArn(t.getTopicArn()));
			}
		}
	}
	
	public void dropmeVpc(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop VPC with tag prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		Filter f = new Filter().withName("tag:Name").withValues(prefix+"*");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.dropVpcByFilter(ec2, f);
	}
	
	public void dropmeEbs(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop EBS with tag prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		Filter f = new Filter().withName("tag:Name").withValues(prefix+"*");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.dropEbsByFilter(ec2, f);
	}
	
	public void dropmeSnapshot(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop Snapshot with tag prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		Filter f = new Filter().withName("tag:Name").withValues(prefix+"*");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.dropSnapshotByFilter(ec2,f);
	}
	
	public void dropmeElb(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop ELB with prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		AmazonElasticLoadBalancing elb = (AmazonElasticLoadBalancing) Clients.getClientByProfile(Clients.ELB, profile);
		ELBUtil util = new ELBUtil();
		List<LoadBalancerDescription> descs = elb.describeLoadBalancers().getLoadBalancerDescriptions();
		for(LoadBalancerDescription desc:descs){
			if(desc.getLoadBalancerName().startsWith(prefix)){
				util.deleteElbByName(elb, desc.getLoadBalancerName());
			}
		}
	}
	
	public void dropmeSg(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop SG with name prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		Filter f = new Filter().withName("group-name").withValues(prefix+"*");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.dropSecurityGroupByFilter(ec2, f);
	}
	
	public void dropmeEc2(String prefix, String profile) throws Exception{
		h.help(prefix,"<object-prefix-to-clean> <profile>");
		System.out.println("> Drop EC2 with tag prefix "+prefix+"* in "+Biu.PROFILE_REGIONS.get(profile).getName());
		Filter f = new Filter().withName("tag:Name").withValues(prefix+"*");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.terminateInstancesByFilter(ec2, f);		
	}

	public void showActionByService(String serviceName) throws Exception{
		h.help(serviceName,"<service-name>");
		Object u = Clients.getClientByProfile(SERVICE_PACK_NAMES.get(serviceName),"global");
		Class<?> clazz = u.getClass();
		System.out.println("# Actions can be called by "+clazz.getCanonicalName());
		Method[] methods = clazz.getDeclaredMethods();
		TreeSet<String> ts = new TreeSet<String>(); 
		for(Method m:methods){
			if(Modifier.isPublic(m.getModifiers())){
				ts.add(m.getName());
			}
		}
		Iterator<String> it = ts.iterator();
		while(it.hasNext()){
			System.out.println(it.next());
		}
	}
	
	public void deleteBucketForce(String regionPartition, String bucketName) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.deleteBucketForce(s3, bucketName);
	}
	
	public void clearMultipartUploadTrash(String regionPartition, String bucketName) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		TransferManager tm = new TransferManager(s3); 
		S3Util util = new S3Util();
		util.clearMultipartTrash(tm, bucketName);
	}
	
	public void uploadFileMultipartSizeParallel(String regionPartition, String bucketName, String key, String filePath, String partSizeInMB, String dop) throws Exception{
		h.help(regionPartition, "<region-partition: china|others> <bucket> <key> <local-file-path> <part-size-in-mb> <degree-of-parallel>");
		S3Util util = new S3Util();
		util.uploadFileMultipartSizeParallel(regionPartition, new File(filePath), bucketName, key, Integer.parseInt(partSizeInMB), Integer.parseInt(dop));
	}
	
	public void uploadFileMultipartPartParallel(String regionPartition, String bucketName, String key, String filePath, String partCount, String dop) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path> <part-count> <degree-of-parallel>");
		S3Util util = new S3Util();
		util.uploadFileMultipartParallel(regionPartition, new File(filePath), bucketName, key, Integer.parseInt(partCount), Integer.parseInt(dop));
	}
	
	public void uploadFileMultipart(String regionPartition, String bucketName, String key, String filePath, String partCount) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path> <part-count>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.uploadFileMultipart(s3, new File(filePath), bucketName, key, Integer.parseInt(partCount));
	}
	
	public void uploadFile(String regionPartition, String bucketName, String key, String filePath) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.uploadFile(s3, new File(filePath), bucketName, key);
	}
	
	public void uploadFileWithCustomerKey(String regionPartition, String bucketName, String key, String filePath, String customerKeySerDePath) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path> <customerKeySerDePath>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.uploadFileWithCustomerEncryptionKey(s3, new File(filePath), bucketName, key, customerKeySerDePath);
	}
	
	public void downloadFile(String regionPartition, String bucketName, String key, String filePath) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.downloadFile(s3, bucketName, key, new File(filePath));
	}
	
	public void downloadFileWithCustomerKey(String regionPartition, String bucketName, String key, String filePath, String customerKeySerDePath) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path> <customerKeySerDePath>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		util.downloadFileWithCustomerEncryptionKey(s3, bucketName, key, new File(filePath), customerKeySerDePath);
	}
	
	public void downloadFileAnonymous(String regionPartition, String bucketName, String key, String filePath) throws Exception{
		h.help(regionPartition,"<region-partition: china|others> <bucket> <key> <local-file-path>");
		S3Util util = new S3Util();
		if(regionPartition.equals("china")){
			util.downloadFileAnonymousChina(bucketName, key, new File(filePath));
		}
		else{
			util.downloadFileAnonymousGlobal(bucketName, key, new File(filePath));
		}
	}
	
	public void showAmiByPrefix(String amiNamePrefix, String profile) throws Exception{
		h.help(amiNamePrefix,"<ami-name-prefix> <profile>");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		List<Image> images = util.getAmiByName(ec2, amiNamePrefix);
		for(Image image:images){
			System.out.println(image.getName()+", "+image.getImageId()+", "+image.getCreationDate()+", "+image.getImageLocation());
		}
	}
	
	public void removeAmiById(String amiId, String profile) throws Exception{
		h.help(amiId,"<ami-id> <profile>");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		util.deregisterAmi(ec2, amiId);
		System.out.println("Removing AMI: "+amiId+" requested");
	}
	
	public void ddbDeleteGsi(String tableName, String indexName, String profile) throws Exception{
		h.help(tableName,"<table-name> <index-name> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.deleteGsi(ddb, indexName, tableName);
	}
	
	public void ddbCreateGsi(String tableName, String indexName, String hashName, String rangeName, String rcu, String wru, String profile) throws Exception{
		h.help(tableName,"<table-name> <index-name> <hash-attr-name> <range-attr-name> <read-unit> <write-unit> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		String rangeKeyName = rangeName.equals("null")?null:rangeName;
		util.createGsiString(ddb, indexName, tableName, hashName, rangeKeyName, Long.parseLong(rcu), Long.parseLong(wru));
	}
	
	public void ddbUpdateItemByPkHashString(String regionName, String tableName, String hashName, String hashValue, String updateExpression, String profile) throws Exception{
		h.help(regionName,"<table-name> <hash-pk-name> <hash-pk-value> <update-expression|replace space with ^> <profile>");
		String expandedUpdateExpression = updateExpression.replaceAll("\\^", " ");
		System.out.println("UpdateExpression: "+expandedUpdateExpression);
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.updateItemByPkHashString(ddb, tableName, hashName, hashValue, expandedUpdateExpression);
	}
	
	/* Should use an application for demo.
	private void putItemConditionalToDdb(String regionName, String tableName, String hashName, String hashValue, String rangeName, String rangeValue, String conditionExpression) throws Exception{
		h.help(regionName,"*<region> <table-name> <hash-pk-name> <hash-pk-value> <range-pk-name> <range-pk-value> <condition-expression>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClient(Clients.DDB, Regions.fromName(regionName));
		DynamoDBUtil util = new DynamoDBUtil();
		HashMap<String,AttributeValue> item = new HashMap<String,AttributeValue>();
		item.put(hashName,new AttributeValue().withS(hashValue));
		item.put(rangeName,new AttributeValue().withS(rangeValue));
		HashMap<String,String> expressionAttributeNames = new HashMap<String,String>();
		expressionAttributeNames.put("#n","name");
		util.putItemConditional(ddb, tableName, item, expressionAttributeNames,conditionExpression);
	}*/
	
	/* Should use an application for demo.
	private void putDocumentItemToDdb(String regionName, String tableName, String hashName, String hashValue, String rangeName, String rangeValue) throws Exception{
		h.help(regionName,"*<region> <table-name> <hash-pk-name> <hash-pk-value> <range-pk-name> <range-pk-value>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClient(Clients.DDB, Regions.fromName(regionName));
		DynamoDBUtil util = new DynamoDBUtil();
		Item item = new Item().withPrimaryKey(hashName, hashValue, rangeName, rangeValue)
								.withString("attr1", "value1")
								.withInt("attr2", 10);
		util.putDocumentItem(ddb, tableName, item);
	}*/
	
	public void ddbScanItemByFilterAsync(String tableName, String filterName, String filterValue, String profile) throws Exception{
		h.help(tableName,"<table-name> <filter-name> <filter-value> <profile>");
		AmazonDynamoDBAsync ddb = (AmazonDynamoDBAsync) Clients.getClientByProfile(Clients.DDBAsync, "profile");
		DynamoDBUtil util = new DynamoDBUtil();
		util.scanItemByFilterAsync(ddb,tableName,filterName,filterValue);
	}
	
	public void ddbScanItemByFilter(String tableName, String filterName, String filterValue, String pageSize, String profile) throws Exception{
		h.help(tableName,"<table-name> <filter-attribute-name> <filter-attribute-value> <page-size> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.scanItemByFilter(ddb,tableName,filterName,filterValue, Integer.parseInt(pageSize));
	}
	
	public void ddbScanItemByFilterParallel(String tableName, String filterName, String filterValue, String degreeOfParallel, String profile) throws Exception{
		h.help(tableName,"<table-name> <filter-attribute-name> <filter-attribute-value> <parallel-degree> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.scanItemByFilterParallel(ddb,tableName,filterName,filterValue,Integer.parseInt(degreeOfParallel));
	}
	
	public void ddbQueryByHashRangeBeginsWithString(String tableName, String indexName, String hashName, String hashValue, String rangeName, String rangeValue, String ascOrDesc, String profile) throws Exception{
		h.help(tableName,"<table-name> <index-name|null:(Using PK)> <hash-name> <hash-value> <range-name> <range-begins-with-value> <asc|desc> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		if(indexName.equals("null")){indexName=null;}
		util.queryItemByHashStringRangeString(ddb, tableName, hashName, hashValue, rangeName, rangeValue, indexName, ascOrDesc.equals("asc")?true:false);
	}
	
	public void ddbQueryByHashString(String tableName, String indexName, String pkName, String pkValue, String profile) throws Exception{
		h.help(tableName,"<table-name> <indexName|null> <hash-pk-name> <hash-pk-value> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		if(indexName.equals("null")){indexName=null;}
		util.queryItemByHashString(ddb, tableName, pkName, pkValue, indexName);
	}
	
	public void ddbGetItemByPkHashString(String tableName, String pkName, String pkValue, String consistentRead, String profile) throws Exception{
		h.help(tableName,"<table-name> <hash-pk-name> <hash-pk-value> <consistent-read> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.getItemByPkHashString(ddb, tableName, pkName, pkValue,consistentRead.equals("true"));
	}

	public void ddbGetItemByPkHashRangeString(String tableName, String pk1Name, String pk1Value, String pk2Name, String pk2Value, String consistentRead, String profile) throws Exception{
		h.help(tableName,"<table-name> <hash-pk-name> <hash-pk-value> <range-pk-name> <range-pk-value> <consistent-read> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.getItemByPkHashRangeString(ddb, tableName, pk1Name, pk1Value, pk2Name, pk2Value, consistentRead.equals("true"));
	}
	
	
	public void ddbPutItemStringCrazy(String tableName, String pkHashName, String pkRangeName, String profile) throws Exception{
		h.help(tableName,"<table-name> <hash-pk-name> <range-pk-name> <profile>");
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		String pkRangeNameReal = pkRangeName.equals("null")?null:pkRangeName;
		util.putItemCrazy(ddb, tableName, pkHashName, pkRangeNameReal);
	}
	
	/**
	 * Only for String type attributes.
	 * @param regionTableItemAttributeAndValue 0:profile, 1:tableName
	 * @throws Exception 
	 */
	public void ddbPutItemString(String[] regionTableItemAttributeAndValue) throws Exception{
		//h.help(regionTableItemAttributeAndValue[0],"<region-name> <table-name> <attr-key> <attr-value> ...");
		String profile = regionTableItemAttributeAndValue[0];
		String tableName = regionTableItemAttributeAndValue[1];
		HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		for(int i=2;i<regionTableItemAttributeAndValue.length;i=i+2){
			item.put(regionTableItemAttributeAndValue[i], new AttributeValue().withS(regionTableItemAttributeAndValue[i+1]));
		}
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		util.putItem(ddb, tableName, item);
	}
	
	public void ddbDeleteItemString(String[] regionTableItemPkAttributeAndValue) throws Exception{
		//h.help(regionTableItemAttributeAndValue[0],"<profile> <table-name> <pk-attr-key> <pk-attr-value> ...");
		String profile = regionTableItemPkAttributeAndValue[0];
		String tableName = regionTableItemPkAttributeAndValue[1];
		HashMap<String, AttributeValue> pk = new HashMap<String, AttributeValue>();
		for(int i=2;i<regionTableItemPkAttributeAndValue.length;i=i+2){
			pk.put(regionTableItemPkAttributeAndValue[i], new AttributeValue().withS(regionTableItemPkAttributeAndValue[i+1]));
		}
		AmazonDynamoDB ddb = (AmazonDynamoDB) Clients.getClientByProfile(Clients.DDB, profile);
		DynamoDBUtil util = new DynamoDBUtil();
		System.out.println("WARNING: deleting dynamodb item...");
		util.deleteItemByPkString(ddb, tableName, pk);		
	}
	
	public void showObjectInBucket(String regionPartition, String bucketName, String keyPrefix) throws Exception{	
		h.help(regionPartition,"<region-partition: china|orthers> <bucket> <key-prefix>");
		AmazonS3 s3 = (AmazonS3) Clients.getIdeologyClient(Clients.S3, regionPartition);
		S3Util util = new S3Util();
		if(keyPrefix.equals("null")){
			keyPrefix=null;
		}
		util.listObjectsInBucket(s3, bucketName, keyPrefix);
	}
	
	public void showDefaultClientConfig(){
		ClientConfiguration cc = new ClientConfiguration();
		RetryPolicy rp = cc.getRetryPolicy();
		System.out.println(
					"Connection Timeout - Initially: "+cc.getConnectionTimeout()+"\n"+
							"Connection TTL - Expire in Pool: "+cc.getConnectionTTL()+"\n"+
							"Max Connections: "+cc.getMaxConnections()+"\n"+
							"Max Error Retry (5xx): "+cc.getMaxErrorRetry()+"\n"+
							"Protocol: "+cc.getProtocol().name()+"\n"+
							"Retry Policy: "+rp.toString().replaceAll("@.*$","")+"\n"+
							"Retry Policy - Max Error Retry: "+rp.getMaxErrorRetry()+"\n"+
							"Retry Policy - BackoffStrategy: "+rp.getBackoffStrategy().toString().replaceAll("@.*$","")+"\n"+
							"Retry Policy - Honor Max Error Retry (5xx): "+(rp.isMaxErrorRetryInClientConfigHonored()?"YES":"NO")+"\n"+
							"Signer Override: "+cc.getSignerOverride()+"\n"+
							"Socket Buffer Size Hint: (send) "+cc.getSocketBufferSizeHints()[0]+", (receive) "+cc.getSocketBufferSizeHints()[1]+"\n"+
							"Socket Timeout: "+cc.getSocketTimeout()+"\n"+
							"User Agent: "+cc.getUserAgent()
				);
	}
	
	/**
	 * @param fileLocalPath
	 * @param validDay
	 * @return
	 */
	public void shareToPublicChina(String fileLocalPath, String validDurationInDays) throws Exception{
		h.help(fileLocalPath,"<file-local-path> <valid-days>");
		AmazonS3 s3 = (AmazonS3) Clients.getClientByProfile(Clients.S3, "beijing");
		String basename = fileLocalPath.substring(fileLocalPath.lastIndexOf("/"));
		String key = "working-share"+basename;
		System.out.println("Key: "+key);
		PutObjectRequest por = new PutObjectRequest(CONFIG_BACKUP_BUCKET,key,new File(fileLocalPath));
		PutObjectResult pors = s3.putObject(por);
		String etag = pors.getETag();
		System.out.println("ETag: "+etag);
		S3Util s3util = new S3Util();
		URL url = s3util.getPresignedUrl(CONFIG_BACKUP_BUCKET,key,Integer.valueOf(validDurationInDays)*24,"GET", "beijing");
		String presignedUrl = url.toString();
		System.out.println(presignedUrl);
	}
	
	public void generatePresignedUrl(String profile, String bucketName, String key, String validDurationInDays) throws Exception{
		h.help(bucketName,"<bucket-name> <key> <valid-days> <profile>");
		S3Util s3util = new S3Util();
		URL url = null;
		url = s3util.getPresignedUrl(bucketName,key,Integer.valueOf(validDurationInDays)*24,"GET", profile);
		System.out.println(url.toString());
	}
	
	public void generatePresignedMethodUrl(String bucketName, String key, String validDurationInDays, String method, String profile) throws Exception{
		h.help(bucketName,"<bucket-name> <key> <valid-days> <method> <profile>");
		S3Util s3util = new S3Util();
		URL url = null;
		int vd = Integer.parseInt(validDurationInDays);
		AmazonS3 s3 = (AmazonS3) Clients.getClientByProfile(Clients.S3, profile);
		url = s3util.getPresignedUrl(s3, bucketName, key, vd*24, method);
		System.out.println(url.toString());
	}
	
	public void closeAllNetwork() throws Exception{
		this.closeBeijingNetwork();
		this.closeTokyoNetwork();
		this.closeVirginiaNetwork();
	}
	
	/**
	 * shutdown bjs public network.
	 * @throws Exception
	 */
	public void closeBeijingNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "beijing");
		EC2Util util = new EC2Util();
		util.denyAllIngressOnNACL(ec2, CONFIG_APP_NACL_BEIJING);
		util.denyAllIngressOnNACL(ec2, CONFIG_DEFAULT_NACL_BEIJING);
	}
	
	public void closeVirginiaNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "virginia");
		EC2Util util = new EC2Util();
		util.denyAllIngressOnNACL(ec2, CONFIG_APP_NACL_VIR);
	}
	
	public void closeTokyoNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "tokyo");
		EC2Util util = new EC2Util();
		util.denyAllIngressOnNACL(ec2, CONFIG_APP_NACL_TOKYO);
		util.denyAllIngressOnNACL(ec2, CONFIG_DEFAULT_NACL_TOKYO);
	}
	
	
	/**
	 * open bjs public network.
	 * @throws Exception
	 */
	public void openBjsNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "beijing");
		EC2Util util = new EC2Util();
		util.removeIngressNo49(ec2, CONFIG_APP_NACL_BEIJING);
		util.removeIngressNo49(ec2, CONFIG_DEFAULT_NACL_BEIJING);
	}
	
	public void openVirNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "virginia");
		EC2Util util = new EC2Util();
		util.removeIngressNo49(ec2, CONFIG_APP_NACL_VIR);
	}
	
	public void openTokyoNetwork() throws Exception{
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "tokyo");
		EC2Util util = new EC2Util();
		util.removeIngressNo49(ec2, CONFIG_APP_NACL_TOKYO);
		util.removeIngressNo49(ec2, CONFIG_DEFAULT_NACL_TOKYO);
	}
	
	public void showAllService() throws IllegalArgumentException, IllegalAccessException{
		Class<?> clazz = ServiceAbbreviations.class;
		Field[] fields = clazz.getDeclaredFields();
		TreeSet<String> set = new TreeSet<String>();
		for(Field f:fields){
			set.add(f.getName()+" - "+f.get(null));
		}
		int i=1;
		for(String s:set){
			System.out.println(i+++": "+s);
		}
	}
	
	public static void showRegionByService(String serviceAbb){
		h.help(serviceAbb,"<service-name>");
		System.out.println("_______\\");
		System.out.println("Service: "+serviceAbb);
		List<Region> regions = RegionUtils.getRegionsForService(serviceAbb);
		int i=0;
		for(Region r:regions){
			System.out.println(++i+") "+r.getName());
		}
	}
	
	public void showInstanceType() throws Exception{
		int i = 1;
		for (InstanceType type : InstanceType.values()){
			System.out.println(i+++") "+ type.toString());
		}
	}
	
	public String base64Encode(String plain){
		h.help(plain,"<plain-text>");
		byte[] b = null;
		String base64 = null;
		try {
			b=plain.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if(b!=null){
			base64 = new BASE64Encoder().encode(b);
		}
		System.out.println(base64);
		return base64;
	}
	
	public String base64Decode(String base64){
		h.help(base64,"<base64-text>");
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
		System.out.println(plain);
		return plain;
	}
	
	public String base64EncodeFromFile(String file){
		h.help(file,"<local-file-path>");
		StringBuffer sb = new StringBuffer();
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(file)));
			line = br.readLine();
			while(line!=null){
				sb.append(line+"\n");
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String script = new String(sb);
		System.out.println("== File Content:");
		System.out.println(script);
		if(script.equals("")){
			throw new RuntimeException("File Content is NULL!!");
		}
		String base64 = this.base64Encode(script);
		return base64;
	}
	
	public void changeAsgAmiSoft(String asgName, String ami, String rollingWaitMins, String profile) throws Exception{
		h.help(asgName,"<asg-name> <ami-id> <rolling-terminate-wait-minutes> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.changeAMIForAsg(aas, asgName, ami, false, Integer.parseInt(rollingWaitMins));
	}
	
	public void changeAsgInstaneTypeSoft(String asgName, String instanceType, String rollingWaitMins, String profile) throws Exception{
		h.help(asgName,"<asg-name> <instane-type> <rolling-terminate-wait-minutes> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.changeInstanceTypeForAsg(aas, asgName, instanceType, false, Integer.parseInt(rollingWaitMins));
	}
	
	public void changeAsgUserdataSoft(String asgName, String userdataFilePath, String rollingWaitMins, String profile) throws Exception{
		h.help(asgName,"<asg-name> <userdata-file-path> <rolling-terminate-wait-minutes> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.changeUserdataForAsg(aas, asgName, this.base64EncodeFromFile(userdataFilePath), false, Integer.parseInt(rollingWaitMins));
	}
	
	public void changeAsgLcSoft(String asgName, String lcName, String rollingWaitMins, String profile) throws Exception{
		h.help(asgName,"<asg-name> <launch-config-name> <rolling-terminate-wait-minutes> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.changeLaunchConfigurationForAsg(aas, asgName, lcName, false, Integer.parseInt(rollingWaitMins));
	}
	
	public void swapAsgLcSoft(String asgName, String rollingWaitMins, String profile) throws Exception{
		h.help(asgName,"<asg-name> <rolling-terminate-wait-minutes> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.swapLaunchConfigurationForAsg(aas, asgName, false, Integer.parseInt(rollingWaitMins));
	}
	
	public void showAsgLc(String asgName, String profile) throws Exception{
		h.help(asgName,"<asg-name> <profile>");
		ASGUtil util = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		util.printLaunchConfigurationForAsg(aas, asgName);
	}
	
	public void yumUpdateAsgSoftBeijing(String asgName, String waitMins) throws Exception{
		h.help(asgName,"<asg-name> <wait-mins-during-swap-members>");
		ASGUtil au = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, "beijing");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "beijing");
		String[] commands = {"sleep 1"};
		if(au.checkNewLaunchConfigAvail(aas, asgName)){
			au.commandsToAmiForAsgByName(aas, ec2, CONFIG_BEIJING_EC2_KEYPAIR_NAME, asgName, CONFIG_BEIJING_DEFAULT_VPC_SUBNET1, CONFIG_BEIJING_DEFAULT_VPC_SUBNET2, CONFIG_BEIJING_DEFAULT_VPC_ALLOWALL_SECURITY_GROUP, commands, Integer.parseInt(waitMins),false);
		}
		else{
			System.out.println("Next launch configuration name is occupied, please have a check.");
		}
	}
	
	/**
	 * This routine will eventually create new LC with a version increasing name.
	 * So, if the new name is not available, abort the mission. 
	 * @param asgName
	 * @param waitMins
	 * @param commands
	 * @throws Exception
	 */
	public void commandsToAmiAsgSoftBeijing(String... commands) throws Exception{
		//h.help(asgName,"<asg-name> <wait-mins-during-swap-members>, <command1, command2, ...>");
		ASGUtil au = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, "beijing");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, "beijing");
		if(au.checkNewLaunchConfigAvail(aas, commands[0])){
			au.commandsToAmiForAsgByName(aas, ec2, CONFIG_BEIJING_EC2_KEYPAIR_NAME, commands[0], CONFIG_BEIJING_DEFAULT_VPC_SUBNET1, CONFIG_BEIJING_DEFAULT_VPC_SUBNET2, CONFIG_BEIJING_DEFAULT_VPC_ALLOWALL_SECURITY_GROUP, commands, Integer.parseInt(commands[1]),false);
		}
		else{
			System.out.println("Next launch configuration name is occupied, please have a check.");
		}
	}
	
	public void diffLc(String lc1, String lc2, String profile) throws Exception{
		h.help(lc1,"<launch-config-#1> <launch-config-#2> <profile>");
		ASGUtil au = new ASGUtil();
		AmazonAutoScaling aas = (AmazonAutoScaling) Clients.getClientByProfile(Clients.ASG, profile);
		Map<String,String> diff = au.getLaunchConfigSideBySide(aas, lc1, lc2);
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(diff.keySet());
		Collections.sort(keys);
		String[] compareArray = null;
		String[] userdataArray = new String[2];
		for(String key:keys){
			if(key.startsWith("UserData for ")){
				if(userdataArray[0]==null){
					userdataArray[0] = diff.get(key);
				}
				else{
					userdataArray[1] = diff.get(key);
					if( !userdataArray[0].equals(userdataArray[1])){
						System.out.println("("+key+")\t"+userdataArray[1]);
						System.out.println("(Other)\t"+userdataArray[0]);
					}
				}
			}
			else{
				compareArray = diff.get(key).split(":");
				if(compareArray!=null && compareArray.length==2 && !compareArray[0].equals(compareArray[1])){
					System.out.println("("+key+")\t"+diff.get(key));
				}
			}
		}
	}
	
	public void showDependentSg(String sgid, String profile) throws Exception{
		h.help(sgid,"<security-group-id> <profiles>");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		for(String sid:util.getDependentSecurityGroupIds(ec2, sgid)){
			System.out.println(sid+": "+ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(sid)).getSecurityGroups().get(0).getGroupName());
		}
	}
	
	public void showReferencingSg(String sgid, String profile) throws Exception{
		h.help(sgid,"<security-group-id> <profiles>");
		AmazonEC2 ec2 = (AmazonEC2) Clients.getClientByProfile(Clients.EC2, profile);
		EC2Util util = new EC2Util();
		for(String sid:util.getReferencingSecurityGroupIds(ec2, sgid)){
			System.out.println(sid+": "+ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(sid)).getSecurityGroups().get(0).getGroupName());
		}
	}

	
	public static void coreV2(String[] args) throws Exception{
		
		ArrayList<String> skipMethods = new ArrayList<String>();
		skipMethods.add("main");skipMethods.add("coreV2");
		
		if(args==null || args.length==0){
			System.out.println("Usage:");
			TreeSet<String> ts = new TreeSet<String>();
			StringBuffer sb = null;
			Method[] allMethods = Biu.class.getDeclaredMethods();
			ArrayList<Method> methods = new ArrayList<Method>();
			for(Method m:allMethods){
				if(Modifier.isPublic(m.getModifiers())){
					methods.add(m);
				}
			}
			String mn = null;
			for(Method m:methods){
				sb = new StringBuffer();
				mn = m.getName();
				if(skipMethods.contains(mn)){
					continue;
				}
				sb.append(mn+": ");
				int parameterCount = m.getParameterTypes().length;
				for(int i=0;i<parameterCount;i++){
					sb.append("[]");
				}
				ts.add(new String(sb));
			}
			for(String s:ts){
				System.out.println(s);
			}
			return;
		}
		
		Biu u = new Biu();
		Class<?> clazz = u.getClass();
		Method[] allMethods = clazz.getDeclaredMethods();
		ArrayList<Method> methods = new ArrayList<Method>();
		for(Method m:allMethods){
			if(Modifier.isPublic(m.getModifiers())){
				methods.add(m);
			}
		}
		if(skipMethods.contains(args[0])){
			System.out.println("options unkown.");
			return;
		}
		for(Method m:methods){
			if (m.getName().equals(args[0])){
				System.out.println(m.getName()+" :: "+args[0]);
				// Pass through #1: putItemToDdb, #2: deleteItemFromDdb
				if(args[0].equals("ddbDeleteItemString")){
					String[] parameters = Arrays.copyOfRange(args, 1, args.length);
					if(parameters[0].equals("-h")){
						System.out.println("<profile> <table-name> <pk-attr-name> <pk-attr-value> ...");
						return;
					}
					u.ddbDeleteItemString(parameters);
					return;
				}
				if(args[0].equals("ddbPutItemString")){
					String[] parameters = Arrays.copyOfRange(args, 1, args.length);
					if(parameters[0].equals("-h")){
						System.out.println("<profile> <table-name> <attr-key-name> <attr-value> ...");
						return;
					}
					u.ddbPutItemString(parameters);
					return;
				}
				if(args[0].equals("commandsToAmiAsgSoftBeijing")){
					String[] parameters = Arrays.copyOfRange(args, 1, args.length);
					if(parameters[0].equals("-h")){
						System.out.println("<asg-name> <wait-mins-during-swapping>, \\\"<command1>\\\", \\\"<command2>\\\", \\\"<...>\\\"");
						return;
					}
					u.commandsToAmiAsgSoftBeijing(parameters);
					return;
				}
				// Options filter
				Class<?>[] paramTypes = m.getParameterTypes();
				if(paramTypes==null || paramTypes.length==0 || paramTypes[0]!=(new String[]{"XXX"}.getClass())){
					int paramCount = paramTypes.length;
					String[] paramValues = new String[paramCount];
					// Take '-h' help into consideration.
					for(int i=0;i<paramValues.length;i++){
						if(args[1].equals("-h")){
							paramValues[0] = args[1];
							for(int j=1;j<paramValues.length;j++){
								paramValues[j] = null;
							}
							break;
						}
						else{
							paramValues[i] = args[i+1];
						}
					}
					m.invoke(u, (Object[])paramValues);
				}
				else{
					String[] mParameters = Arrays.copyOfRange(args, 1, args.length);
					System.out.println(m.getName()+": "+Arrays.toString(mParameters));
					m.invoke(u, (Object[])(mParameters));
				}
				return;
			}
		}
		System.out.println("options unkown.");
		return;
	}
	
	public static void main(String[] args) throws Exception{
		coreV2(args);
	}
}
