package bglutil.common;


import java.nio.ByteBuffer;
import java.util.Arrays;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;

public class KMSUtil {
	public void printAllPhysicalId(AWSKMS kms){
		for(AliasListEntry entry:kms.listAliases().getAliases()){
			System.out.println("key-alias: "+entry.getAliasName()+", "+entry.getAliasArn());
		}
	}
	
	
	public void generateDataKeyAndDecrypt(AWSKMS kms, String keyId){
		// Generate Data Key.
		GenerateDataKeyResult res = kms.generateDataKey(
								new GenerateDataKeyRequest()
								.withKeySpec("AES_256")
								.withKeyId(keyId));
		
		ByteBuffer plainTextKey = res.getPlaintext();
		ByteBuffer encryptedKey = res.getCiphertextBlob();
		
		System.out.println("# Plain Text:");
		System.out.println(Arrays.toString(plainTextKey.array()));
		System.out.println("# Ciphertext Blob:");
		System.out.println(Arrays.toString(encryptedKey.array()));
		
		// Decrypt Data Key.
		DecryptRequest req = new DecryptRequest()
							.withCiphertextBlob(encryptedKey);
		
		ByteBuffer plainTextDecrypted = kms.decrypt(req).getPlaintext();
		
		System.out.println("# Plain Text Decrypted:");
		System.out.println(Arrays.toString(plainTextDecrypted.array()));
	}
	
}
