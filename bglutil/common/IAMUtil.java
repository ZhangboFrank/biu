package bglutil.common;

import java.util.List;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupRequest;
import com.amazonaws.services.identitymanagement.model.DeleteInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesForRoleRequest;
import com.amazonaws.services.identitymanagement.model.ListPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Policy;
import com.amazonaws.services.identitymanagement.model.PolicyScopeType;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.User;

public class IAMUtil {
	
	public void printAllPhysicalId(AmazonIdentityManagement iam){
		for(User u: iam.listUsers().getUsers()){
			System.out.println("user: "+u.getUserName());
		}
		for(Group g:iam.listGroups().getGroups()){
			System.out.println("group: "+g.getGroupName());
		}
		for(Role r:iam.listRoles().getRoles()){
			System.out.println("role: "+r.getRoleName());
		}
		for(Policy p:iam.listPolicies(new ListPoliciesRequest().withScope(PolicyScopeType.Local)).getPolicies()){
			System.out.println("cust-policy: "+p.getPolicyName());
		}
		for(InstanceProfile ip: iam.listInstanceProfiles().getInstanceProfiles()){
			System.out.println("inst-profile: "+ip.getInstanceProfileName());
		}
	}
	
	public void dropUser(AmazonIdentityManagement iam, String username){
		try{
			iam.deleteLoginProfile(new DeleteLoginProfileRequest().withUserName(username));
		}catch(NoSuchEntityException ex){
		}
		List<AccessKeyMetadata> akms = iam.listAccessKeys(new ListAccessKeysRequest().withUserName(username)).getAccessKeyMetadata();
		for(AccessKeyMetadata akm:akms){
			iam.deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId(akm.getAccessKeyId()).withUserName(username));
		}
		for(String policy:iam.listUserPolicies(new ListUserPoliciesRequest().withUserName(username)).getPolicyNames()){
			iam.deleteUserPolicy(new DeleteUserPolicyRequest().withPolicyName(policy).withUserName(username));
			System.out.println("Deleting user policy - "+policy);
		}
		iam.deleteUser(new DeleteUserRequest().withUserName(username));
		System.out.println("Dropping user - "+username);
	}
	
	public void dropGroup(AmazonIdentityManagement iam, String groupname){
		for(String policy:iam.listGroupPolicies(new ListGroupPoliciesRequest().withGroupName(groupname)).getPolicyNames()){
			iam.deleteGroupPolicy(new DeleteGroupPolicyRequest().withPolicyName(policy).withGroupName(groupname));
			System.out.println("Deleting group policy - "+policy);
		}
		iam.deleteGroup(new DeleteGroupRequest().withGroupName(groupname));
		System.out.println("Dropping group - "+groupname);
	}
	
	public void dropRole(AmazonIdentityManagement iam, String rolename){
		List<InstanceProfile> ips = iam.listInstanceProfilesForRole(new ListInstanceProfilesForRoleRequest().withRoleName(rolename)).getInstanceProfiles();
		for(InstanceProfile ip:ips){
			iam.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest()
				.withRoleName(rolename)
				.withInstanceProfileName(ip.getInstanceProfileName()));
		}
		for(String policy:iam.listRolePolicies(new ListRolePoliciesRequest().withRoleName(rolename)).getPolicyNames()){
			iam.deleteRolePolicy(new DeleteRolePolicyRequest().withPolicyName(policy).withRoleName(rolename));
			System.out.println("Deleting role policy - "+policy);
		}
		iam.deleteRole(new DeleteRoleRequest().withRoleName(rolename));
		System.out.println("Dropping role - "+rolename);
	}
	
	public void dropInstanceProfile(AmazonIdentityManagement iam, String instanceProfileName){
		iam.deleteInstanceProfile(new DeleteInstanceProfileRequest().withInstanceProfileName(instanceProfileName));
	}
}
