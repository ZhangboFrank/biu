package bglutil.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SSECustomerKey;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

public class S3Util {
	
	public void copyBucket(AmazonS3 s3, String sourceBucketName, String destinationBucketName){
		String sourceKey = null;
		CopyObjectResult res = null;
		
		ListObjectsRequest lor = new ListObjectsRequest().withBucketName(
				sourceBucketName).withMaxKeys(100);
		System.out.println("\n# Start: ");
		ObjectListing ol = null;
		int i = 0;
		do {
			ol = s3.listObjects(lor);
			
			for (S3ObjectSummary objectSummary : ol.getObjectSummaries()) {
				sourceKey = objectSummary.getKey();
				System.out.println(++i + ". Object: " + sourceKey
						+ "  " + objectSummary.getStorageClass() + " "
						+ "(size = " + objectSummary.getSize() + ") ");
				res = s3.copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, sourceKey));
				System.out.println(sourceKey+" copied, last modified at "+ res.getLastModifiedDate().toString());
			}
			if (ol.isTruncated()) {
				lor.setMarker(ol.getNextMarker());
			}
		} while (ol.isTruncated());
		System.out.println("# End. ");
	}
	
	public void uploadFileWithCustomerEncryptionKey(AmazonS3 s3, File file, String bucketName, String key, String keySerDeFileName) throws NoSuchAlgorithmException, IOException{
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256);
		SecretKey sk = kg.generateKey();
		FileOutputStream fos = new FileOutputStream(new File(keySerDeFileName));
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(sk);
		oos.close();
		PutObjectRequest req = new PutObjectRequest(bucketName, key, file)
								.withSSECustomerKey(new SSECustomerKey(sk));
		req.setGeneralProgressListener(new ProgressListener() {
			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				System.out.println("Event Code: "
						+ progressEvent.getEventType().toString() + ", "
						+ "Bytes transfered: "
						+ progressEvent.getBytesTransferred());
			}
		});
		s3.putObject(req);
		System.out.println("File uploaded.");
	}
	
	public void downloadFileWithCustomerEncryptionKey(AmazonS3 s3, String bucketName, String key, File saveFilePath, String keySerDeFileName) throws IOException, ClassNotFoundException{
		System.out.println("Downloading s3://"+bucketName+"/"+key+" using key stored in "+keySerDeFileName);
		FileInputStream fis = new FileInputStream(new File(keySerDeFileName));
		ObjectInputStream ois = new ObjectInputStream(fis);
		SecretKey sk = (SecretKey) ois.readObject();
		
		GetObjectRequest req = new GetObjectRequest(bucketName,key)
								.withSSECustomerKey(new SSECustomerKey(sk));
		
		S3Object o = s3.getObject(req);
		S3ObjectInputStream s3is = o.getObjectContent();
		
		byte[] buffer = new byte[128];
		try {
			FileOutputStream fw = new FileOutputStream(saveFilePath);
			try {
				int len = s3is.read(buffer);
				while (len != -1) {
					fw.write(buffer, 0, len);   // to specify offset, otherwise the file downloaded will be corrupted.
					len = s3is.read(buffer);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				fw.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			s3is.close();
		}
		System.out.println("Saved to "+saveFilePath.getAbsolutePath());
	}

	public void uploadFile(AmazonS3 s3, File file, String bucketName, String key) {
		PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
		req.setGeneralProgressListener(new ProgressListener() {
			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				System.out.println("Event Code: "
						+ progressEvent.getEventType().toString() + ", "
						+ "Bytes transfered: "
						+ progressEvent.getBytesTransferred());
			}
		});
		s3.putObject(req);
		System.out.println("File uploaded.");
	}
	
	public void deleteBucketForce(AmazonS3 s3, String bucketName){
		// Suspend versioning. Pity, version suspending cannot be issued by SDK.
		
		// Deleting all objects.
		List<KeyVersion> keys = new ArrayList<KeyVersion>();
		ListObjectsRequest req  = new ListObjectsRequest().withBucketName(bucketName);
		ObjectListing ol = s3.listObjects(req);
    	for (S3ObjectSummary obj : ol.getObjectSummaries()) {
    	    keys.add(new KeyVersion(obj.getKey()));
    	    System.out.println("Deleting "+obj.getKey());
    	}
    	if(keys.size()>0){
    		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
    		deleteObjectsRequest.withKeys(keys);
    		s3.deleteObjects(deleteObjectsRequest);
    	}
    	while(ol.isTruncated()){
    		req.setMarker(ol.getNextMarker());
    		ol = s3.listObjects(req);
        	for (S3ObjectSummary obj : ol.getObjectSummaries()) {
        	    keys.add(new KeyVersion(obj.getKey()));
        	    System.out.println("Deleting "+obj.getKey());
        	}
        	if(keys.size()>0){
        		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
        		deleteObjectsRequest.withKeys(keys);
        		s3.deleteObjects(deleteObjectsRequest);
        	}
    	}
    	// Deleting all versions.
    	List<KeyVersion> versions = new ArrayList<KeyVersion>();
    	ListVersionsRequest lvr = new ListVersionsRequest()
    									.withBucketName(bucketName);
    	VersionListing vl = s3.listVersions(lvr);
    	for (S3VersionSummary ver : vl.getVersionSummaries()) {
    	    versions.add(new KeyVersion(ver.getKey()));
    	    System.out.println("Deleting "+ver.getKey()+", "+ver.getVersionId());
    	    DeleteVersionRequest deleteVersionRequest = new DeleteVersionRequest(bucketName,ver.getKey(),ver.getVersionId());
    		s3.deleteVersion(deleteVersionRequest);
    	}
    	
    	DeleteBucketRequest dbr = new DeleteBucketRequest(bucketName);
    	s3.deleteBucket(dbr);
    	System.out.println("Bucket deleted.");
	}
	
	public void clearMultipartTrash(TransferManager tm, String bucketName) {
		int oneMinute = 1000 * 60;
		Date oneMinuteAgo = new Date(System.currentTimeMillis() - oneMinute);
		tm.abortMultipartUploads(bucketName, oneMinuteAgo);
		System.out.println("Cleared.");
	}
	
	public void uploadFileMultipartSizeParallel(String location, File file,
		String bucketName, String key, int partSizeInMB, int dop) throws Exception{
		long fileSize = file.length();
		int partCount = (int) fileSize/(partSizeInMB*1024*1024) + 1;
		partCount = partCount % 2 == 0 ? partCount : partCount - 1; 
		this.uploadFileMultipartParallel(location, file, bucketName, key, partCount, dop);
	}

	public void uploadFileMultipartParallel(String profile, File file,
			String bucketName, String key, int partCount, int dop)
			throws Exception {
		
		if(!file.exists()){System.out.println("File does NOT exist!"); System.exit(-1);}
		
		System.out.println("Partcount: "+partCount+", DOP: "+dop);
		
		if(partCount % dop !=0 ){
			System.out.println("dop % partCount != 0, modifying...");
			int piece = 1;
			while((partCount+piece) % dop != 0){
				piece++;
			}
			partCount = partCount + piece;
			System.out.println("Partcount: "+partCount+", DOP: "+dop);
		}
		
		List<PartETag> partETags = Collections
				.synchronizedList(new ArrayList<PartETag>());
		// Init.
		AmazonS3 s3 = (AmazonS3)Clients.getClientByProfile(Clients.S3, profile);
		
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
				bucketName, key);
		InitiateMultipartUploadResult initResponse = s3
				.initiateMultipartUpload(initRequest);
		long contentLength = file.length();
		long partSize = contentLength / partCount + 1L;
		// Upload.
		long filePosition = 0;
		long startTime = System.currentTimeMillis();
		MultipartUploadWorker[] workers = new MultipartUploadWorker[dop];
		int partIdx = 1;
		for (int workerSequence = 0; workerSequence < workers.length && filePosition < contentLength; workerSequence++, partIdx++) {
			workers[workerSequence] = new MultipartUploadWorker(profile,
					partSize, contentLength, filePosition, partIdx, bucketName,
					key, initResponse, file, partETags, workerSequence);
			workers[workerSequence].start();
			filePosition += partSize; // Advance.
			
		}
		while (filePosition < contentLength) {
			for (int s = 0; s < workers.length; s++) {
				if (!workers[s].isAlive()) {
					workers[s] = new MultipartUploadWorker(profile, partSize,
							contentLength, filePosition, partIdx, bucketName,
							key, initResponse, file, partETags, s);
					workers[s].start();
					partIdx++;
					filePosition += partSize; // Advance.
				}
			}
		}
		// Close.
		for (MultipartUploadWorker worker : workers) {
			worker.join();
		}
		CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
				bucketName, key, initResponse.getUploadId(), partETags);
		s3.completeMultipartUpload(compRequest);
		long endTime = System.currentTimeMillis();
		long ela = (endTime - startTime) / 1000;
		System.out.println("Uploaded in " + ela + "s, avg " + contentLength * 8
				/ 1024 / 1024 / ela + "Mb/sec");
		System.out.println("Done.");
	}

	public void uploadFileMultipart(AmazonS3 s3, File file, String bucketName,
			String key, int partCount) {
		List<PartETag> partETags = new ArrayList<PartETag>();
		// Init.
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
				bucketName, key);
		InitiateMultipartUploadResult initResponse = s3
				.initiateMultipartUpload(initRequest);
		long contentLength = file.length();
		long partSize = contentLength / partCount + 1L;
		// Upload.
		long filePosition = 0;
		long startTime = System.currentTimeMillis();
		for (int i = 1; filePosition < contentLength; i++) {
			partSize = Math.min(partSize, (contentLength - filePosition));
			System.out.println("Part " + i + " size will be: " + partSize);
			UploadPartRequest uploadRequest = new UploadPartRequest()
					.withBucketName(bucketName).withKey(key)
					.withUploadId(initResponse.getUploadId()).withPartNumber(i)
					.withFileOffset(filePosition).withFile(file)
					.withPartSize(partSize);

			partETags.add(s3.uploadPart(uploadRequest).getPartETag()); // Gather
																		// the
																		// uploaded
																		// ETag.

			filePosition += partSize; // Advance.

		}
		long endTime = System.currentTimeMillis();
		long ela = (endTime - startTime) / 1000;
		System.out.println("Uploaded in " + ela + "s, avg " + contentLength * 8
				/ 1024 / 1024 / ela + "Mb/sec");

		// Close.
		CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
				bucketName, key, initResponse.getUploadId(), partETags);
		s3.completeMultipartUpload(compRequest);
		System.out.println("Done.");
	}

	public void downloadFileAnonymousChina(String bucketName, String key, File saveFilePath) throws Exception{
		AmazonS3 s3 = new AmazonS3Client(new AnonymousAWSCredentials());
		s3.setRegion(Region.getRegion(Regions.CN_NORTH_1));
		this.downloadFile(s3, bucketName, key, saveFilePath);
	}
	
	public void downloadFileAnonymousGlobal(String bucketName, String key, File saveFilePath) throws Exception{
		AmazonS3 s3 = new AmazonS3Client(new AnonymousAWSCredentials());
		s3.setRegion(Region.getRegion(Regions.US_EAST_1));
		this.downloadFile(s3, bucketName, key, saveFilePath);
	}
	
	public void downloadFile(AmazonS3 s3, String bucketName, String key, File saveFilePath) throws IOException{
		
		GetObjectRequest req = new GetObjectRequest(bucketName,key);
		S3Object o = s3.getObject(req);
		
		S3ObjectInputStream s3is = o.getObjectContent();
		
		byte[] buffer = new byte[128];
		try {
			FileOutputStream fw = new FileOutputStream(saveFilePath);
			try {
				int len = s3is.read(buffer);
				while (len != -1) {
					fw.write(buffer, 0, len);   // to specify offset, otherwise the file downloaded will be corrupted.
					len = s3is.read(buffer);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				fw.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			s3is.close();
		}
	}

	/**
	 * Generic pre-signed URL.
	 * 
	 * @param s3
	 * @param bucketName
	 * @param key
	 * @param hour
	 * @param httpMethod
	 * @return
	 */
	public URL getPresignedUrl(AmazonS3 s3, String bucketName, String key,
			int hour, String httpMethod) {
		Date expiration = new Date();
		long milliSeconds = expiration.getTime();
		milliSeconds += hour * 1000 * 60 * 60;
		expiration.setTime(milliSeconds);
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
				bucketName, key);
		generatePresignedUrlRequest.setMethod(HttpMethod.valueOf(httpMethod));
		generatePresignedUrlRequest.setExpiration(expiration);
		URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);
		return url;
	}

	public void listObjectsInBucket(AmazonS3 s3, String bucketName,
			String keyPrefix) {
		System.out.print("Objects in " + bucketName);
		ListObjectsRequest lor = new ListObjectsRequest().withBucketName(
				bucketName).withMaxKeys(100);
		if (keyPrefix != null) {
			lor.setPrefix(keyPrefix);
			System.out.println("... with prefix " + keyPrefix);
		}
		System.out.println("\n# Start: ");
		ObjectListing ol = null;
		int i = 0;
		do {
			ol = s3.listObjects(lor);
			for (S3ObjectSummary objectSummary : ol.getObjectSummaries()) {
				System.out.println(++i + ". Object: " + objectSummary.getKey()
						+ "  " + objectSummary.getStorageClass() + " "
						+ "(size = " + objectSummary.getSize() + ") ");
			}
			if (ol.isTruncated()) {
				lor.setMarker(ol.getNextMarker());
			}
		} while (ol.isTruncated());
		System.out.println("# End. ");
	}

	public URL getPresignedUrl(String bucketName, String key, int hour,
			String httpMethod, String profile) throws Exception {
		AmazonS3 s3 = (AmazonS3) Clients.getClientByProfile(Clients.S3, profile);
		return this.getPresignedUrl(s3, bucketName, key, hour, httpMethod);
	}

	class MultipartUploadWorker extends Thread {

		private long partSize;
		private long contentLength;
		private long filePosition;
		private List<PartETag> partETags;
		private String bucketName;
		private String key;
		private InitiateMultipartUploadResult initResponse;
		private int partIdx;
		private File file;
		private AmazonS3 s3;
		private int workerSequence;
		private boolean jobDone;

		MultipartUploadWorker(String profile, long partSize,
				long contentLength, long filePosition, int partIdx,
				String bucketName, String key,
				InitiateMultipartUploadResult initResponse, File file,
				List<PartETag> partETags, int workerSequence) {
			this.contentLength = contentLength;
			this.filePosition = filePosition;
			this.partSize = Math.min(partSize,
					(this.contentLength - this.filePosition));
			System.out.println("Partsize: "+this.partSize/1024/1024+" MB");
			this.partETags = partETags;
			this.bucketName = bucketName;
			this.key = key;
			this.initResponse = initResponse;
			this.partIdx = partIdx;
			this.file = file;
			this.workerSequence = workerSequence;
			try {
				this.s3 = (AmazonS3) Clients.getClientByProfile(Clients.S3, profile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Part " + partIdx + " size will be: " + partSize
					+ " upload by worker" + this.workerSequence);
		}

		public void run() {
			UploadPartRequest uploadRequest = new UploadPartRequest()
					.withBucketName(this.bucketName).withKey(this.key)
					.withUploadId(this.initResponse.getUploadId())
					.withPartNumber(this.partIdx)
					.withFileOffset(this.filePosition).withFile(this.file)
					.withPartSize(this.partSize);
			this.partETags.add(this.s3.uploadPart(uploadRequest).getPartETag()); 
			this.jobDone = true;
		}

		public boolean isJobDone() {
			return this.jobDone;
		}

	}
}
