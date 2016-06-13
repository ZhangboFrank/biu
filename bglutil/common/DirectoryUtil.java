package bglutil.common;

import com.amazonaws.services.directory.AWSDirectoryService;
import com.amazonaws.services.directory.model.DirectoryDescription;

public class DirectoryUtil {
	public void printAllPhysicalId(AWSDirectoryService directory){
		
		for(DirectoryDescription dd:directory.describeDirectories().getDirectoryDescriptions()){
			System.out.println("directory-id: "+dd.getDirectoryId()+", directory-name: "+dd.getName()+", "+dd.getAccessUrl());
		}
	}
	
	public void createUser(AWSDirectoryService directory, String username){
		
	}
}
