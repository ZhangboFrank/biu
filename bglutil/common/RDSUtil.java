package bglutil.common;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;

public class RDSUtil {
	public void printAllPhysicalId(AmazonRDS rds){
		for(DBInstance instance:rds.describeDBInstances().getDBInstances()){
			System.out.println("rds: "+instance.getEndpoint().getAddress()+":"+instance.getEndpoint().getPort()+"/"+instance.getDBName());
		}
		for(DBSnapshot snapshot:rds.describeDBSnapshots().getDBSnapshots()){
			System.out.println("rds-snapshot: "+snapshot.getDBSnapshotIdentifier()+", "+snapshot.getSnapshotType());
		}
	}
}
