package wang.liangchen.matrix.easycache.sdk.override;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import wang.liangchen.matrix.easycache.sdk.cache.CacheManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Liangchen.Wang 2022-07-04 13:43
 */
class MatrixCacheResolver extends SimpleCacheResolver {
    private final org.springframework.cache.CacheManager springCacheManager;

    public MatrixCacheResolver(org.springframework.cache.CacheManager springCacheManager) {
        super(springCacheManager);
        this.springCacheManager = springCacheManager;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Duration ttl = null;
        if (springCacheManager instanceof CacheManager) {
            BasicOperation operation = context.getOperation();
            if (operation instanceof MatrixCacheableOperation) {
                ttl = ((MatrixCacheableOperation) operation).getTtl();
            } else if (operation instanceof MatrixCachePutOperation) {
                ttl = ((MatrixCachePutOperation) operation).getTtl();
            }
        }
        Collection<String> cacheNames = getCacheNames(context);
        Collection<Cache> caches = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            if (null == ttl || Duration.ZERO == ttl) {
                Cache cache = springCacheManager.getCache(cacheName);
                caches.add(cache);
            } else {
                Cache cache = ((CacheManager) springCacheManager).getCache(cacheName, ttl);
                caches.add(cache);
            }
        }
        return caches;
    }
}
