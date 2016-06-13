package bglutil.common.kinesis;

import java.nio.ByteBuffer;
import java.util.List;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.KinesisClientLibDependencyException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;

public class KCLRecordsPrinter implements IRecordProcessor{
	
	private String shardId;

	
	@Override
	public void initialize(String shardId) {
		this.shardId = shardId;
		System.out.println("KCL Stream Processor for Shard:"+this.shardId+" initialized.");
	}

	@Override
	public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
		ByteBuffer bb = null;
		String partitionKey = null;
		String sequenceNumber = null;
		for(Record r:records){
			bb = r.getData();
			partitionKey = r.getPartitionKey();
			sequenceNumber = r.getSequenceNumber();
			System.out.println("Record: part#:"+partitionKey+", seq#:"+sequenceNumber+", data:"+bb.toString());
		}
		try {
			checkpointer.checkpoint(); // Marking all the records up to this point are processed.
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
	public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
		String chk = "chk no";
		if(reason.equals(ShutdownReason.TERMINATE)){
			try {
				checkpointer.checkpoint();
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
		System.out.println(chk+", Shard#: "+this.shardId+" shutdown with reason: "+reason.toString());
	}
	
}
