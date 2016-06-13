package bglutil.common.kinesis;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

public class KCLRecordsPrinterFactory implements IRecordProcessorFactory{
   
	
	@Override
	public IRecordProcessor createProcessor() {
		return new KCLRecordsPrinter();
	}

}
