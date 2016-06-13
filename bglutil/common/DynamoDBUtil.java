package bglutil.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class DynamoDBUtil {
	
	public void printAllPhysicalId(AmazonDynamoDB ddb){
		for(String tablename:ddb.listTables().getTableNames()){
			System.out.println("dynamodb: "+tablename);
		}
	}
	
	public void deleteTable(AmazonDynamoDB ddb, String tableName) {
		DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
				.withTableName(tableName);
		DeleteTableResult deleteTableResult = ddb
				.deleteTable(deleteTableRequest);
		System.out.println("Table " + tableName + " :"
				+ deleteTableResult.getTableDescription().getTableStatus());
	}
	
	public void deleteGsi(AmazonDynamoDB ddb, String indexName, String tableName){
		DeleteGlobalSecondaryIndexAction deleteGsiAction = new DeleteGlobalSecondaryIndexAction()
															.withIndexName(indexName);
		GlobalSecondaryIndexUpdate gsiUpdate = new GlobalSecondaryIndexUpdate().withDelete(deleteGsiAction);	
		
		UpdateTableRequest updateTableRequest = new UpdateTableRequest()
												.withTableName(tableName)
												.withGlobalSecondaryIndexUpdates(gsiUpdate);
		
		UpdateTableResult res = ddb.updateTable(updateTableRequest);
		for(GlobalSecondaryIndexDescription desc: res.getTableDescription().getGlobalSecondaryIndexes()){
			System.out.println("GSI remains: "+desc.getIndexName());
		}
	}
	
	public void createGsiString(AmazonDynamoDB ddb, String indexName, String tableName, String hashName, String rangeName, long readCapacityUnits, long writeCapacityUnits) throws Exception{
		CreateGlobalSecondaryIndexAction createGsiAction = new CreateGlobalSecondaryIndexAction()
												.withIndexName(indexName)
												.withProjection(new Projection().withProjectionType(ProjectionType.ALL))
												.withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(readCapacityUnits).withWriteCapacityUnits(writeCapacityUnits));
											
		KeySchemaElement hashKey = new KeySchemaElement().withAttributeName(hashName).withKeyType(KeyType.HASH);
		HashSet<KeySchemaElement> keys = new HashSet<KeySchemaElement>();
		keys.add(hashKey);
		HashSet<AttributeDefinition> attrDefs = new HashSet<AttributeDefinition>();
		AttributeDefinition hashAd = new AttributeDefinition()
										.withAttributeName(hashName)
										.withAttributeType(ScalarAttributeType.S);
		attrDefs.add(hashAd);
		
		if(rangeName!=null){
			KeySchemaElement rangeKey = new KeySchemaElement().withAttributeName(rangeName).withKeyType(KeyType.RANGE);
			keys.add(rangeKey);
			AttributeDefinition rangeAd = new AttributeDefinition()
			.withAttributeName(rangeName)
			.withAttributeType(ScalarAttributeType.S);
			attrDefs.add(rangeAd);
		}
		createGsiAction.setKeySchema(keys);
												
		GlobalSecondaryIndexUpdate gsiUpdate = new GlobalSecondaryIndexUpdate().withCreate(createGsiAction);
		
		UpdateTableRequest updateTableRequest = new UpdateTableRequest()
												.withTableName(tableName)
												.withGlobalSecondaryIndexUpdates(gsiUpdate)
												.withAttributeDefinitions(attrDefs);
		
		UpdateTableResult res = ddb.updateTable(updateTableRequest);
		for(GlobalSecondaryIndexDescription desc: res.getTableDescription().getGlobalSecondaryIndexes()){
			System.out.println("GSI: "+desc.getIndexName());
		}
	}
	
	public void updateItemByPkHashString(AmazonDynamoDB ddb, String tableName, String hashName, String hashValue, 
			String updateExpression){
		HashMap<String,AttributeValue> pk = new HashMap<String,AttributeValue>();
		pk.put(hashName, new AttributeValue().withS(hashValue));
		
		UpdateItemRequest req = new UpdateItemRequest()
								.withTableName(tableName)
								.withKey(pk)
								.withUpdateExpression(updateExpression)
								.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		UpdateItemResult res = ddb.updateItem(req);
		System.out.println("UpdateItem done, consumed: "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	public void updateItemByHashPkString(AmazonDynamoDB ddb, String tableName, String hashName, String hashValue, 
			String updateExpression, 
			Map<String,String> expressionAttributeNames, 
			Map<String, AttributeValue> expressionAttributeValues){
		
		HashMap<String,AttributeValue> pk = new HashMap<String,AttributeValue>();
		pk.put(hashName, new AttributeValue().withS(hashValue));
		
		UpdateItemRequest req = new UpdateItemRequest()
								.withTableName(tableName)
								.withKey(pk)
								.withUpdateExpression(updateExpression)
								.withExpressionAttributeNames(expressionAttributeNames)
								.withExpressionAttributeValues(expressionAttributeValues)
								.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		UpdateItemResult res = ddb.updateItem(req);
		System.out.println("UpdateItem done, consumed: "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	public void updateItem(AmazonDynamoDB ddb, String tableName, String hashName, String hashValue, String rangeName, String rangeValue, String updateExpression, Map<String,String> expressionAttributeNames, Map<String, AttributeValue> expressionAttributeValues){
		HashMap<String,AttributeValue> pk = new HashMap<String,AttributeValue>();
		pk.put(hashName, new AttributeValue().withS(hashValue));
		pk.put(rangeName, new AttributeValue().withS(rangeValue));
		
		UpdateItemRequest req = new UpdateItemRequest()
								.withTableName(tableName)
								.withKey(pk)
								.withUpdateExpression(updateExpression)
								.withExpressionAttributeNames(expressionAttributeNames)
								.withExpressionAttributeValues(expressionAttributeValues)
								.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		UpdateItemResult res = ddb.updateItem(req);
		System.out.println("UpdateItem done, consumed: "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	/**
	 * Scan a ddb serially.
	 * @param ddb
	 * @param tableName
	 * @param filterName
	 * @param filterValue
	 */
	public void scanItemByFilter(AmazonDynamoDB ddb, String tableName, String filterName, String filterValue, int limit){
		Condition condition = new Condition()
		.withComparisonOperator(ComparisonOperator.CONTAINS)
		.withAttributeValueList(new AttributeValue().withS(filterValue));
		ScanRequest sr = new ScanRequest()
				.withTableName(tableName)
				.addScanFilterEntry(filterName, condition)
				.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
				.withLimit(limit);
		ScanResult srr = ddb.scan(sr);
		int page = 1;
		System.out.println("# Page "+page+": ");
		this.printScanResult(srr);
		System.out.println("# Scanned Count: "+srr.getScannedCount());
		while(srr.getLastEvaluatedKey()!=null){
			sr.setExclusiveStartKey(srr.getLastEvaluatedKey());
			srr = ddb.scan(sr);
			System.out.println("# Page "+(++page)+": ");
			this.printScanResult(srr);
			System.out.println("# Scanned Count: "+srr.getScannedCount());
		}
	}
	
	public void scanItemByFilterAsync(AmazonDynamoDBAsync ddbAsync, String tableName, String filterName, String filterValue) throws InterruptedException, ExecutionException{
		
		Condition condition = new Condition()
		.withComparisonOperator(ComparisonOperator.CONTAINS)
		.withAttributeValueList(new AttributeValue().withS(filterValue));
		ScanRequest sr = new ScanRequest()
				.withTableName(tableName)
				.addScanFilterEntry(filterName, condition)
				.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		Future<ScanResult> f =  ddbAsync.scanAsync(sr, new AsyncHandler<ScanRequest,ScanResult>(){
			@Override
			public void onError(Exception exception) {
				System.out.println("scanItemByFilterAsync call on failed with exception: "+exception.getMessage());
			}
			@Override
			public void onSuccess(ScanRequest request, ScanResult result) {
				System.out.println("\n>> onSuccess callback received.");
				System.out.println("# Printing: ");
				List<Map<String,AttributeValue>> items = result.getItems();
				System.out.println("# Result: ");
				for(Map<String,AttributeValue> item: items){
					for(String attribute: item.keySet()){
						System.out.print(attribute+":"+item.get(attribute)+" ");
					}
					System.out.println();
				}
				System.out.println("# Consumed Capacity: "+result.getConsumedCapacity().getCapacityUnits());
			}});
		long startTime = System.currentTimeMillis();
		System.out.println("Doing something after scanAsync(ScanRequest, AsyncHandler<ScanRequest,ScanResult>) invocation.");
		ScanResult srr = f.get();
		long endTime = System.currentTimeMillis();
		System.out.println("\n>> Future<ScanResult> received after "+(endTime-startTime)+" ms");
		this.printScanResult(srr);
	}
	
	/**
	 * Scan a ddb in parallel.
	 * @param ddb
	 * @param tableName
	 * @param filterName
	 * @param filterValue
	 * @param degreeOfParallel
	 * @throws Exception
	 */
	public void scanItemByFilterParallel(AmazonDynamoDB ddb, String tableName, String filterName, String filterValue, int degreeOfParallel) throws Exception{
		
		Thread[] workers = new Thread[degreeOfParallel];
		System.out.println("Table: "+tableName);
		System.out.println("Filter: "+filterName);
		System.out.println("Value: "+filterValue);
		for(int i=0;i<workers.length;i++){
			//AmazonDynamoDB client = (AmazonDynamoDB) Clients.getChinaClient(Clients.DDB, Regions.fromName("cn-north-1"));
			workers[i] = new ScanWorker(ddb,tableName,filterName,filterValue,i,workers.length);
			workers[i].start();
			System.out.println("Scan worker "+i+" started.");
		}
		for(Thread worker: workers){
			worker.join();
		}
		System.out.println("Job done.");
	}
	
	/**
	 * Range value begins with.
	 * @param ddb
	 * @param tableName
	 * @param hashName
	 * @param hashValue
	 * @param rangeName
	 * @param rangeValue
	 */
	public void queryItemByHashStringRangeString(AmazonDynamoDB ddb, String tableName, String hashName, String hashValue, String rangeName, String rangeValue, String indexName, boolean asc){
		AttributeValue searchHashValue = new AttributeValue().withS(hashValue);
		AttributeValue searchRangeValue = new AttributeValue().withS(rangeValue);
		Condition eqHashCondition = new Condition()
									.withComparisonOperator(ComparisonOperator.EQ)
									.withAttributeValueList(searchHashValue);
		Condition startsWithRangeCondition = new Condition()
									.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
									.withAttributeValueList(searchRangeValue);
		QueryRequest req = new QueryRequest()
									.withTableName(tableName)
									.addKeyConditionsEntry(hashName,eqHashCondition)
									.addKeyConditionsEntry(rangeName,startsWithRangeCondition)
									.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
									.withIndexName(indexName)
									.withScanIndexForward(asc);
		QueryResult res = ddb.query(req);
		this.printQueryResult(res);
		while(res.getLastEvaluatedKey()!=null){
			req.setExclusiveStartKey((res.getLastEvaluatedKey()));
			res = ddb.query(req);
			this.printQueryResult(res);
		}
	}
	
	public void getItemByPkHashRangeString(AmazonDynamoDB ddb, String tableName, String pkHashName, String pkHashValue, String pkRangeName, String pkRangeValue, boolean consistentRead){
		HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(pkHashName, new AttributeValue().withS(pkHashValue));
		key.put(pkRangeName, new AttributeValue().withS(pkRangeValue));
		GetItemRequest req = new GetItemRequest()
							.withTableName(tableName)
							.withKey(key)
							.withConsistentRead(consistentRead)
							.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		GetItemResult res = ddb.getItem(req);
		if(consistentRead){
			System.out.println("CR turned on...");
		}
		else{
			System.out.println("CR turned off...");
		}
		this.printGetItemResult(res);
	}
	
	public void getItemByPkHashString(AmazonDynamoDB ddb, String tableName, String pkName, String pkValue, boolean consistentRead){
		HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(pkName, new AttributeValue().withS(pkValue));
		GetItemRequest req = new GetItemRequest()
							.withTableName(tableName)
							.withKey(key)
							.withConsistentRead(consistentRead)
							.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		GetItemResult res = ddb.getItem(req);
		if(consistentRead){
			System.out.println("CR turned on...");
		}
		else{
			System.out.println("CR turned off...");
		}
		this.printGetItemResult(res);
	}
	
	public void queryItemByHashString(AmazonDynamoDB ddb, String tableName, String pkName, String pkValue, String indexName){
		AttributeValue searchValue = new AttributeValue().withS(pkValue);
		Condition condition = new Condition()
								.withComparisonOperator(ComparisonOperator.EQ)
								.withAttributeValueList(searchValue);
		QueryRequest req = new QueryRequest()
								.withTableName(tableName)
								.addKeyConditionsEntry(pkName,condition)
								.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
								.withIndexName(indexName);
		QueryResult res = ddb.query(req);
		this.printQueryResult(res);
	}
	
	private void printGetItemResult(GetItemResult res){
		Map<String,AttributeValue> item = res.getItem();
		if(item==null){System.out.println("No match result"); return;}
		for(String attribute: item.keySet()){
			System.out.print(attribute+":"+item.get(attribute)+" ");
		}
		System.out.println("# Consumed Capacity: "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	private void printItem(Map<String, AttributeValue> item){
		if(item==null){System.out.println("Item is null"); return;}
		for(String attribute: item.keySet()){
			System.out.print(attribute+":"+item.get(attribute)+" ");
		}
	}
	
	private void printScanResult(ScanResult srr){
		System.out.println("# Printing: ");
		List<Map<String,AttributeValue>> items = srr.getItems();
		System.out.println("# Result: ");
		for(Map<String,AttributeValue> item: items){
			for(String attribute: item.keySet()){
				System.out.print(attribute+":"+item.get(attribute)+" ");
			}
			System.out.println();
		}
		System.out.println("# Consumed Capacity: "+srr.getConsumedCapacity().getCapacityUnits());
	}
	
	private void printQueryResult(QueryResult res){
		List<Map<String,AttributeValue>> items = res.getItems();
		if(items==null || items.size()==0){System.out.println("No match result"); return;}
		System.out.println("# Result: ");
		for(Map<String,AttributeValue> item: items){
			for(String attribute: item.keySet()){
				System.out.print(attribute+":"+item.get(attribute)+" ");
			}
			System.out.println();
		}
		System.out.println("# Consumed Capacity: "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	public void putItemConditional(AmazonDynamoDB ddb, String tableName, Map<String, AttributeValue> item, Map<String,String> expressionAttributeNames, String conditionExpression){
		PutItemRequest req = new PutItemRequest()
		.withTableName(tableName)
		.withItem(item)
		.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
		.withConditionExpression(conditionExpression)
		.withExpressionAttributeNames(expressionAttributeNames);
		long startTime = System.currentTimeMillis();
		PutItemResult res = ddb.putItem(req);
		long endTime = System.currentTimeMillis();
		System.out.println("ela: "+(endTime-startTime)+": "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	public void putDocumentItem(AmazonDynamoDB ddb, String tableName, Item item){
		DynamoDB db = new DynamoDB(ddb);
		Table table = db.getTable(tableName);
		table.putItem(item);
	}
	
	public void putItem(AmazonDynamoDB ddb, String tableName, Map<String, AttributeValue> item){
		PutItemRequest req = new PutItemRequest()
							.withTableName(tableName)
							.withItem(item)
							.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		long startTime = System.currentTimeMillis();
		PutItemResult res = ddb.putItem(req);
		long endTime = System.currentTimeMillis();
		System.out.println("ela: "+(endTime-startTime)+"ms : "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	public void deleteItemByPkString(AmazonDynamoDB ddb, String tableName, Map<String, AttributeValue> itemKey){
		DeleteItemRequest req = new DeleteItemRequest()
									.withTableName(tableName)
									.withKey(itemKey)
									.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
									.withReturnValues(ReturnValue.ALL_OLD);
		long startTime = System.currentTimeMillis();
		DeleteItemResult res = ddb.deleteItem(req);
		long endTime = System.currentTimeMillis();
		System.out.print("Deleted Item: ");
		this.printItem(res.getAttributes());
		System.out.println("\nela: "+(endTime-startTime)+"ms : "+res.getConsumedCapacity().getCapacityUnits());
	}
	
	/**
	 * @param ddb
	 * @param tableName
	 */
	public void putItemCrazy(AmazonDynamoDB ddb, String tableName, String pkPartitionName, String pkRangeName){
		PutItemRequest req = new PutItemRequest()
								.withTableName(tableName)
								.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
		long startTime = 0L;
		long endTime = 0L;
		HashMap<String,AttributeValue> item = new HashMap<String, AttributeValue>();
		long i = 0L;
		PutItemResult res = null;
		while(true){
			item.clear();
			item.put(pkPartitionName,new AttributeValue().withS(Long.toString(i++)));
			if(pkRangeName!=null){
				item.put(pkRangeName,new AttributeValue().withS(Long.toString(i++)));
			}
			/*
			AttributeValue v = new AttributeValue();
			v.setS(Integer.toString(v.hashCode()));
			item.put("simple-name",v);*/
			req.setItem(item);
			startTime = System.currentTimeMillis();
			res = ddb.putItem(req);
			endTime = System.currentTimeMillis();
			if(res==null){
				System.out.println("No response yet.");
				
			}
			System.out.println(i+" ela: "+(endTime-startTime)+" ms "+res.getConsumedCapacity().getCapacityUnits());
		}
	}
}

class ScanWorker extends Thread{
	
	private ScanRequest sr;
	private ScanResult srr;
	private AmazonDynamoDB ddb;
	private int workerIdx;
	
	ScanWorker(AmazonDynamoDB ddb, String tableName, String filterName, String filterValue, int segment, int totalSegments){
		this.workerIdx = segment;
		this.ddb = ddb;
		Condition condition = new Condition()
			.withComparisonOperator(ComparisonOperator.CONTAINS)
			.withAttributeValueList(new AttributeValue().withS(filterValue));
		this.sr = new ScanRequest()
					.withTableName(tableName)
					.withSegment(segment)
					.withTotalSegments(totalSegments)
					.addScanFilterEntry(filterName, condition)
					.withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
	}
	
	public void run(){
		System.out.println("# Worker"+this.workerIdx+" Scanning...");
		this.srr = this.ddb.scan(this.sr);
		int sequence = 0;
		this.printScanResult(this.srr,this.workerIdx, sequence);
		while(this.srr.getLastEvaluatedKey()!=null){
			this.sr.setExclusiveStartKey(this.srr.getLastEvaluatedKey());
			this.srr = this.ddb.scan(this.sr);
			this.printScanResult(this.srr,this.workerIdx,sequence++);
		}
	}
	
	private void printScanResult(ScanResult srr, int workerIdx, int sequence){
		StringBuffer sb = new StringBuffer("# Result, woker:"+workerIdx+", sequence:"+sequence+"\n");
		List<Map<String,AttributeValue>> items = srr.getItems();
		for(Map<String,AttributeValue> item: items){
			for(String attribute: item.keySet()){
				sb.append(attribute+":"+item.get(attribute)+" ");
			}
			sb.append("\n");
		}
		sb.append("# Consumed Capacity: "+srr.getConsumedCapacity().getCapacityUnits()+", Scanned Count: "+this.srr.getScannedCount()+"\n");
		System.out.println(new String(sb));
	}
}
