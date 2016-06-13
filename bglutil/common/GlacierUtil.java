package bglutil.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.ListVaultsRequest;

public class GlacierUtil {
	
	public void printAllPhysicalId(AmazonGlacier glacier){
		for(DescribeVaultOutput voutput:glacier.listVaults(new ListVaultsRequest().withAccountId("-")).getVaultList()){
			System.out.println("glacier-vault: "+voutput.getVaultName()+", archives: "+voutput.getNumberOfArchives());
		}
	}
	
	public void purgeVault(AmazonGlacier glacier, String vaultName){
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String endDate = dateFormat.format(cal.getTime());
		
		InventoryRetrievalJobInput inventoryRetrievalJobInput = new InventoryRetrievalJobInput()
																.withEndDate(endDate);
																		
		JobParameters jobParameters = new JobParameters()
										.withType("inventory-retrieval")
										.withInventoryRetrievalParameters(inventoryRetrievalJobInput);
										//.withSNSTopic(snsArn);
										
		InitiateJobRequest request = new InitiateJobRequest()
										.withAccountId("-")
										.withVaultName(vaultName)
										.withJobParameters(jobParameters);
		
		InitiateJobResult res = glacier.initiateJob(request);
		System.out.println("job-id: "+res.getJobId()+", job-url: "+res.getLocation());
	}
	
}
