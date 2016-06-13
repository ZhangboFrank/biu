package bglutil.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import bglutil.Biu;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class Clients {
	
	// Compute
	public static final String EC2 ="ec2.AmazonEC2Client";
	public static final String LAMBDA = "lambda.AWSLambdaClient";
	public static final String ECS = "ecs.AmazonECSClient";
	public static final String ASG = "autoscaling.AmazonAutoScalingClient";
	public static final String ELB = "elasticloadbalancing.AmazonElasticLoadBalancingClient";
	
	// Storage
	public static final String S3 = "s3.AmazonS3Client";
	public static final String GLACIER = "glacier.AmazonGlacierClient";
	public static final String SGW = "storagegateway.AWSStorageGatewayClient";
	public static final String CF = "cloudfront.AmazonCloudFrontClient";
	public static final String EFS = "elasticfilesystem.AmazonElasticFileSystemClient";
	
	// Database
	public static final String DDB = "dynamodbv2.AmazonDynamoDBClient";
	public static final String DDBAsync = "dynamodbv2.AmazonDynamoDBAsyncClient";
	public static final String RDS = "rds.AmazonRDSClient";
	public static final String ELASTICACHE = "elasticache.AmazonElastiCacheClient";
	public static final String REDSHIFT = "redshift.AmazonRedshiftClient";
	
	// Networking
	public static final String DX = "directconnect.AmazonDirectConnectClient";
	public static final String R53 = "route53.AmazonRoute53Client";
	
	// Administration & Security
	public static final String IAM = "identitymanagement.AmazonIdentityManagementClient";
	public static final String CW = "cloudwatch.AmazonCloudWatchClient";
	public static final String LOGS = "logs.AWSLogsClient";
	public static final String DS = "directory.AWSDirectoryServiceClient";
	public static final String SUPPORT = "support.AWSSupportClient"; // Trusted Advisor
	public static final String CLOUDTRAIL = "cloudtrail.AWSCloudTrailClient";
	public static final String CONFIG = "config.AmazonConfigClient";
	public static final String STS = "securitytoken.AWSSecurityTokenServiceClient";
	public static final String KMS = "kms.AWSKMSClient";
	
	// Deployment & Management
	public static final String CFN = "cloudformation.AmazonCloudFormationClient";
	public static final String EB = "elasticbeanstalk.AWSElasticBeanstalkClient";
	public static final String OPSWORKS = "opsworks.AWSOpsWorksClient";
	public static final String CODEDEPLOY = "codedeploy.AmazonCodeDeployClient";
	
	// Analytics
	public static final String EMR = "elasticmapreduce.AmazonElasticMapReduceClient";
	public static final String KINESIS = "kinesis.AmazonKinesisClient";
	public static final String DATAPIPELINE = "datapipeline.DataPipelineClient";
	public static final String ML = "machinelearning.AmazonMachineLearningClient";
	
	// Application Service
	public static final String SQS = "sqs.AmazonSQSClient";
	public static final String SWF = "simpleworkflow.AmazonSimpleWorkflowClient";
	public static final String TRANSCODER = "elastictranscoder.AmazonElasticTranscoderClient";
	public static final String SES = "simpleemail.AmazonSimpleEmailServiceClient";
	public static final String CLOUDSEARCH = "cloudsearch.AmazonCloudSearchClient";
	//public static final String APPSTREAM; NO AppStream?
	
	
	// Mobile Service
	public static final String SNS = "sns.AmazonSNSClient";
	public static final String COGNITOID = "cognitoidentity.AmazonCognitoIdentityClient";
	public static final String COGNITOSYNC = "cognitosync.AmazonCognitoSyncClient";
	//public static final String MOBILEANALYTICS; NO Mobile Analytics?
	
	// Enterprise Application
	public static final String WORKSPACES = "workspaces.AmazonWorkspacesClient";
	//public static final String WORKDOCS; NO WorkDoc?
	//public static final String WORKEMAIL; NO WorkMail?
	
	private static final String SERVICE_PACKAGE_PREFIX="com.amazonaws.services.";
	
	private static Object newInstance(String className, Class<?>[] paramClasses, Object[] paramObjs) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Class<?> clazz = Class.forName(className);
		Constructor<?> conztructor = clazz.getConstructor(paramClasses);
		Object obj = conztructor.newInstance(paramObjs);
		return obj;
	}
	
	private static void setRegion(Object obj, Regions regions) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Class<?> clazz = obj.getClass();
		Class<?> parent = clazz.getSuperclass();
		Class<?> grand = parent.getSuperclass();
		Method mesod = null;
		try {
			mesod = parent.getDeclaredMethod("setRegion",new Class[]{com.amazonaws.regions.Region.class});
		}catch(NoSuchMethodException ex){
			mesod = grand.getDeclaredMethod("setRegion",new Class[]{com.amazonaws.regions.Region.class});
		}
		mesod.invoke(obj, Region.getRegion(regions));
	}

	public static Object getClientByProfile(String service, String profileName) throws Exception{
		Object c = newInstance(SERVICE_PACKAGE_PREFIX+service,new Class[]{AWSCredentialsProvider.class}, new Object[]{AccessKeys.getCredentialsByProfile(profileName)});
		setRegion(c, Biu.PROFILE_REGIONS.get(profileName));
		return c;
	}

	/**
	 * This method is for a simple style usage to identify China and not China is enough.
	 * @param service
	 * @param ideology
	 * @return The Client
	 * @throws Exception
	 */
	public static Object getIdeologyClient(String service, String ideology) throws Exception{
		if (ideology.equals("china")){
			return Clients.getClientByProfile(service, "beijing");
		}
		else{
			return Clients.getClientByProfile(service, "global");
		}
	}
	
	public static boolean isChinaRegion(Regions regions){
		for(Regions r: Biu.CHINA_REGIONS){
			if(r.equals(regions)){
				return true;
			}
		}
		return false;
	}
}
