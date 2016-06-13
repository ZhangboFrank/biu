package bglutil.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.EC2Actions;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import com.amazonaws.util.json.JSONObject;

public class STSUtil {
	
	public GetCallerIdentityResult getCallerId(AWSSecurityTokenService sts){
		return sts.getCallerIdentity(new GetCallerIdentityRequest());
	}
	
	public BasicSessionCredentials getSessionToken(AWSSecurityTokenService sts, int sec){
		GetSessionTokenRequest sessionTokenReq = new GetSessionTokenRequest()
													.withDurationSeconds(sec);
		GetSessionTokenResult sessionTokenRes = sts.getSessionToken(sessionTokenReq);
		Credentials tokenCredentials = sessionTokenRes.getCredentials();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		return bsc;
	}
	
	public BasicSessionCredentials getSessionTokenMFA(AWSSecurityTokenService sts, int sec, String serialNumber, String tokenCode){
		GetSessionTokenRequest sessionTokenReq = new GetSessionTokenRequest()
													.withDurationSeconds(sec)
													.withSerialNumber(serialNumber)
													.withTokenCode(tokenCode);
		GetSessionTokenResult sessionTokenRes = sts.getSessionToken(sessionTokenReq);
		Credentials tokenCredentials = sessionTokenRes.getCredentials();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		return bsc;
	}
	
	public BasicSessionCredentials getFederatedUserToken(AWSSecurityTokenService sts, int sec, String username, Policy policy){
		/*
		Policy policy = new Policy();
		policy.withStatements(
				new Statement(Effect.Allow)
				.withActions(S3Actions.GetObject)
				.withResources(new Resource(
						"arn:aws:s3:::"+this.fedS3BucketName+"/"+this.fedS3ObjectKey)));
		*/
		GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest()
																.withDurationSeconds(sec)
																.withName(username)
																.withPolicy(policy.toJson());
		GetFederationTokenResult federationTokenResult =
				sts.getFederationToken(getFederationTokenRequest);
		System.out.println("Acting Federated user: "+federationTokenResult.getFederatedUser().getArn());
		
		Credentials tokenCredentials = federationTokenResult.getCredentials();
		
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK);
		return bsc;
	}
	
	public BasicSessionCredentials assumeRole(AWSSecurityTokenService sts, int sec, String roleArn, String sessionName){
		AssumeRoleResult assumeRoleRes = sts.assumeRole(new AssumeRoleRequest()
					.withDurationSeconds(sec)
					.withRoleArn(roleArn)
					.withRoleSessionName(sessionName));

		String assumedRoleUser = assumeRoleRes.getAssumedRoleUser().getArn();
		System.out.println("Assumed role user: "+assumedRoleUser);
		Credentials tokenCredentials = assumeRoleRes.getCredentials();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		
		BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		
		sessionName = assumedRoleUser;
		return bsc;
	}
	
	public BasicSessionCredentials assumeRoleExternal(AWSSecurityTokenService sts, int sec, String roleArn, String sessionName, String externalId){
		AssumeRoleResult assumeRoleRes = sts.assumeRole(new AssumeRoleRequest()
		.withDurationSeconds(sec)
		.withRoleArn(roleArn)
		.withRoleSessionName(sessionName)
		.withExternalId(externalId));
		String assumedRoleUser = assumeRoleRes.getAssumedRoleUser().getArn();
		System.out.println("Assumed-Role-User: "+assumedRoleUser);
		Credentials tokenCredentials = assumeRoleRes.getCredentials();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		return bsc;
	}
	
	public String getFederatedUserAwsConsoleSsoUrlForEc2S3AdminGlobal(AWSSecurityTokenService sts, String username, String issuerUrl) throws Exception{
		GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest()
																.withDurationSeconds(900)
																.withName(username);
		// The S3 & EC2 administrator permissions */
		Policy policy = new Policy();
		policy.withStatements(
				new Statement(Effect.Allow)
				.withActions(S3Actions.AllS3Actions)
				.withResources(new Resource("*")),
				new Statement(Effect.Allow)
				.withActions(EC2Actions.AllEC2Actions)
				.withResources(new Resource("*"))
				);
		
		getFederationTokenRequest.setPolicy(policy.toJson());

		GetFederationTokenResult federationTokenResult =
				sts.getFederationToken(getFederationTokenRequest);
		
		Credentials tokenCredentials = federationTokenResult.getCredentials();
		//String userArn = federationTokenResult.getFederatedUser().getArn();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
	
		String loginURL = null;
		if(true){
		//BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		
		String consoleURL = "https://console.aws.amazon.com/ec2"; 		// Place want to go
		String signInURL = "https://signin.aws.amazon.com/federation"; 	// Place to handle next request
		String sessionJson = String.format(
				"{\"%1$s\":\"%2$s\",\"%3$s\":\"%4$s\",\"%5$s\":\"%6$s\"}",
				"sessionId", tempAK,
				"sessionKey", tempAS,
				"sessionToken", tempTK);
		String getSigninTokenURL = signInURL + "?Action=getSigninToken" +
				"&SessionType=json&Session=" + URLEncoder.encode(sessionJson,"UTF-8");
		URL url = new URL(getSigninTokenURL);
		URLConnection conn = url.openConnection ();
		BufferedReader bufferReader = new BufferedReader(new
		InputStreamReader(conn.getInputStream()));
		String returnContent = bufferReader.readLine();
		String signinToken = new JSONObject(returnContent).getString("SigninToken");
		String signinTokenParameter = "&SigninToken=" + URLEncoder.encode(signinToken,"UTF-8");
		//System.out.println("SigninToken Parameter: "+signinTokenParameter);
		// The issuer parameter is optional, but recommended. Use it to direct users
		// to your sign-in page when their session expires.
		String issuerParameter = "&Issuer=" + URLEncoder.encode(issuerUrl, "UTF-8");
		String destinationParameter = "&Destination=" +
		URLEncoder.encode(consoleURL,"UTF-8");
		loginURL = signInURL + "?Action=login" + signinTokenParameter +
		issuerParameter + destinationParameter;
		//System.out.println(loginURL);
		
		}
		return loginURL;
	}
	
	public String getFederatedUserAwsConsoleSsoUrlForEc2S3AdminChina(AWSSecurityTokenService sts, String username, String issuerUrl) throws Exception{
		
		// Step 1 check username, password.
		// JDBC

		// Step 2
		GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest()
																.withDurationSeconds(900)
																.withName(username);
		
		// Step 3 check authorization database username > permissions.
		// JDBC 
		
		// Step 4
		// The S3 & EC2 administrator permissions */
		Policy policy = new Policy();
		policy.withStatements(
				new Statement(Effect.Allow)
				.withActions(S3Actions.AllS3Actions)
				.withResources(new Resource("*")),
				new Statement(Effect.Allow)
				.withActions(EC2Actions.AllEC2Actions)
				.withResources(new Resource("*"))
				);
		
		getFederationTokenRequest.setPolicy(policy.toJson());

		GetFederationTokenResult federationTokenResult =
				sts.getFederationToken(getFederationTokenRequest);
		
		Credentials tokenCredentials = federationTokenResult.getCredentials();
		//String userArn = federationTokenResult.getFederatedUser().getArn();
		String tempAK = tokenCredentials.getAccessKeyId();
		String tempAS = tokenCredentials.getSecretAccessKey();
		String tempTK = tokenCredentials.getSessionToken();
		/*
		System.out.println("User ARN: "+userArn+"\n"+
						"Federation Access Key: "+tempAK+"\n"+
						"Federation Access Secret: "+tempAS+"\n"+
						"Federation Access Token: "+tempTK+"\n"+
						"Federation Access Expiration: "+tokenCredentials.getExpiration().toString());
		*/
		String loginURL = null;
		if(true){
		//BasicSessionCredentials bsc = new BasicSessionCredentials(tempAK,tempAS,tempTK); 
		
		String consoleURL = "https://console.amazonaws.cn/ec2"; 		// Place want to go
		String signInURL = "https://signin.amazonaws.cn/federation"; 	// Place to handle next request
		String sessionJson = String.format(
				"{\"%1$s\":\"%2$s\",\"%3$s\":\"%4$s\",\"%5$s\":\"%6$s\"}",
				"sessionId", tempAK,
				"sessionKey", tempAS,
				"sessionToken", tempTK);
		String getSigninTokenURL = signInURL + "?Action=getSigninToken" +
				"&SessionType=json&Session=" + URLEncoder.encode(sessionJson,"UTF-8");
		URL url = new URL(getSigninTokenURL);
		URLConnection conn = url.openConnection ();
		BufferedReader bufferReader = new BufferedReader(new
		InputStreamReader(conn.getInputStream()));
		String returnContent = bufferReader.readLine();
		String signinToken = new JSONObject(returnContent).getString("SigninToken");
		String signinTokenParameter = "&SigninToken=" + URLEncoder.encode(signinToken,"UTF-8");
		//System.out.println("SigninToken Parameter: "+signinTokenParameter);
		// The issuer parameter is optional, but recommended. Use it to direct users
		// to your sign-in page when their session expires.
		String issuerParameter = "&Issuer=" + URLEncoder.encode(issuerUrl, "UTF-8");
		String destinationParameter = "&Destination=" +
		URLEncoder.encode(consoleURL,"UTF-8");
		loginURL = signInURL + "?Action=login" + signinTokenParameter +
		issuerParameter + destinationParameter;
		//System.out.println(loginURL);
		
		}
		return loginURL;
	}

}
