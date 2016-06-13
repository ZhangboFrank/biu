package bglutil.common;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DeleteCacheClusterRequest;

public class ElastiCacheUtil {
	
	public void printAllPhysicalId(AmazonElastiCache cache){
		for(CacheCluster cc:cache.describeCacheClusters().getCacheClusters()){
			System.out.println("elasticache: "+cc.getCacheClusterId());
		}
	}
	
	public void dropCacheClusterById(AmazonElastiCache cache, String id){
		DeleteCacheClusterRequest request = new DeleteCacheClusterRequest()
											.withCacheClusterId(id);
		System.out.println("=> Dropping "+id);
		cache.deleteCacheCluster(request);
	}
}
