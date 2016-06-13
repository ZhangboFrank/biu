package bglutil.common.kinesis;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.KinesisClientLibDependencyException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

/**
 * There is adapter object can automatically convert V1 object to V2 object, so this v2.IRecordProcessor is NOT used by factory.
 * @author guanglei
 *
 */
public class KCLRecordsPrinterV2 implements IRecordProcessor{
	
	String shardId;

	@Override
	public void initialize(InitializationInput ii) {
		// TODO Auto-generated method stub
		this.shardId = ii.getShardId();
		System.out.println("Handling shard: "+this.shardId);
		
	}

	@Override
	public void processRecords(ProcessRecordsInput pri) {
		// TODO Auto-generated method stub
		for(Record r:pri.getRecords()){
			System.out.println("Part: "+r.getPartitionKey()+", Seq: "+r.getSequenceNumber()+", "+r.getData());
		}
		try {
			pri.getCheckpointer().checkpoint();
		} catch (KinesisClientLibDependencyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ThrottlingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ShutdownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown(ShutdownInput si) {
		// TODO Auto-generated method stub
		String chk = "chk no";
		if(si.getShutdownReason().equals(ShutdownReason.TERMINATE)){
			try {
				si.getCheckpointer().checkpoint();
				chk = "chk yes";
			} catch (KinesisClientLibDependencyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ThrottlingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ShutdownException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(chk+", Shard#: "+this.shardId+" shutdown with reason: "+si.getShutdownReason().toString());
	}

}
