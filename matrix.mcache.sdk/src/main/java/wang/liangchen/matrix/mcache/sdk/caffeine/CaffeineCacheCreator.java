package wang.liangchen.matrix.mcache.sdk.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * @author LiangChen.Wang
 */
public enum CaffeineCacheCreator {
    /**
     * instance
     */
    INSTANCE;

    public Cache<Object, Object> createNativeCache(Caffeine<Object, Object> cacheBuilder, CacheLoader<Object, Object> cacheLoader, long ttl) {
        if (ttl > 0) {
            cacheBuilder.expireAfterWrite(Duration.ofMillis(ttl));
        } else {
            cacheBuilder.expireAfterWrite(Duration.ZERO);
        }

        return (cacheLoader != null ? cacheBuilder.build(cacheLoader) : cacheBuilder.build());
    }

    public AsyncCache<Object, Object> createNativeAsyncCache(Caffeine<Object, Object> cacheBuilder, CacheLoader<Object, Object> cacheLoader, long ttl) {
        if (ttl > 0) {
            cacheBuilder.expireAfterWrite(Duration.ofMillis(ttl));
        } else {
            cacheBuilder.expireAfterWrite(Duration.ZERO);
        }
        return (cacheLoader != null ? cacheBuilder.buildAsync(cacheLoader) : cacheBuilder.buildAsync());
    }
}
