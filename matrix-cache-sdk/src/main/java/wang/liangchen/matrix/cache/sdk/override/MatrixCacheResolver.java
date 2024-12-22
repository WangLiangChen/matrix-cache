package wang.liangchen.matrix.cache.sdk.override;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import wang.liangchen.matrix.cache.sdk.cache.MatrixCacheManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Liangchen.Wang 2022-07-04 13:43
 * find cache names from the contex, and find cache from the cachemanager
 */
public class MatrixCacheResolver extends SimpleCacheResolver {
    private static final Logger logger = LoggerFactory.getLogger(MatrixCacheResolver.class);
    private final CacheManager cacheManager;

    public MatrixCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
        this.cacheManager = cacheManager;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        if (!(cacheManager instanceof MatrixCacheManager)) {
            return super.resolveCaches(context);
        }
        Collection<String> cacheNames = getCacheNames(context);
        if (null == cacheNames || cacheNames.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<Cache> caches = new ArrayList<>(cacheNames.size());
        Duration ttl = null;
        BasicOperation operation = context.getOperation();
        if (operation instanceof MatrixCacheableOperation) {
            ttl = ((MatrixCacheableOperation) operation).getTtl();
        } else if (operation instanceof MatrixCachePutOperation) {
            ttl = ((MatrixCachePutOperation) operation).getTtl();
        }
        Cache cache;
        for (String cacheName : cacheNames) {
            if (null == ttl || Duration.ZERO == ttl) {
                cache = cacheManager.getCache(cacheName);
            } else {
                cache = ((MatrixCacheManager) cacheManager).getCache(cacheName, ttl);
            }
            caches.add(cache);
        }
        return caches;
    }
}
