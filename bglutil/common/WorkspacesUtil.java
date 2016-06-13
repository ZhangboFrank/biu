package bglutil.common;

import com.amazonaws.services.workspaces.AmazonWorkspaces;
import com.amazonaws.services.workspaces.model.Workspace;

public class WorkspacesUtil {
	public void printAllPhysicalId(AmazonWorkspaces workspaces){
		for(Workspace workspace:workspaces.describeWorkspaces().getWorkspaces()){
			System.out.println("workspaces: "+workspace.getWorkspaceId()+", IP: "+workspace.getIpAddress()+", user: "+workspace.getUserName());
		}
	}
}
