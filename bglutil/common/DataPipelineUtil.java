package bglutil.common;

import com.amazonaws.services.datapipeline.DataPipeline;
import com.amazonaws.services.datapipeline.model.PipelineIdName;

public class DataPipelineUtil {
	public void printAllPhysicalId(DataPipeline datapipeline){
		for(PipelineIdName name: datapipeline.listPipelines().getPipelineIdList()){
			System.out.println("pipeline: "+name.getName()+", "+name.getId());
		}
	}
}