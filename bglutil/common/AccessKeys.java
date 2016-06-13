package bglutil.common;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

public class AccessKeys {
	
	/**
	 * Find a ...
	 * 1. Named profile, 
	 * 2. Default profile,
	 * 3. EC2 instance profile.
	 * @param profileName
	 * @return
	 */
	public static AWSCredentialsProviderChain getCredentialsByProfile(String profileName){
		String selectedProfile = profileName==null?"default":profileName;
		return new AWSCredentialsProviderChain(
					new ProfileCredentialsProvider(selectedProfile),
					new InstanceProfileCredentialsProvider());
	}
}
