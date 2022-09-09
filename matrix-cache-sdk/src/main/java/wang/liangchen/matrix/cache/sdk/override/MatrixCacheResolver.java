package wang.liangchen.matrix.cache.sdk.override;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Liangchen.Wang 2022-07-04 13:43
 */
class MatrixCacheResolver extends SimpleCacheResolver {
    private final CacheManager cacheManager;

    public MatrixCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
        this.cacheManager = cacheManager;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Duration ttl = null;
        BasicOperation operation = context.getOperation();
        if (operation instanceof MatrixCacheableOperation) {
            ttl = ((MatrixCacheableOperation) operation).getTtl();
        } else if (operation instanceof MatrixCachePutOperation) {
            ttl = ((MatrixCachePutOperation) operation).getTtl();
        }

        Collection<String> cacheNames = getCacheNames(context);
        Collection<Cache> caches = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            Cache cache;
            if (null == ttl || Duration.ZERO == ttl) {
                cache = cacheManager.getCache(cacheName);
            } else {
                cache = ((wang.liangchen.matrix.cache.sdk.cache.CacheManager) cacheManager).getCache(cacheName, ttl);
            }
            caches.add(cache);
        }
        return caches;
    }
}
