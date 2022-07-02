package wang.liangchen.matrix.easycache.sdk.cache.caffeine;


import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.lang.Nullable;
import wang.liangchen.matrix.easycache.sdk.cache.AbstractCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/4/15
 */
public class CaffeineCacheManager extends AbstractCacheManager {
    private final Caffeine<Object, Object> cacheBuilder;
    @Nullable
    private final CacheLoader<Object, Object> cacheLoader;

    public CaffeineCacheManager(Caffeine<Object, Object> cacheBuilder, @Nullable CacheLoader<Object, Object> cacheLoader) {
        this.cacheBuilder = cacheBuilder;
        this.cacheLoader = cacheLoader;
    }


    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        return caffeineCache(name, ttl);
    }

    private CaffeineCache caffeineCache(String name, Duration ttl) {
        return new CaffeineCache(name, nativeCache(name, ttl), isAllowNullValues(), ttl);
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(String name, Duration ttl) {
        if (ttl.toNanos() > 0) {
            this.cacheBuilder.expireAfterWrite(ttl);
        }
        return (this.cacheLoader != null ? this.cacheBuilder.build(this.cacheLoader) : this.cacheBuilder.build());
    }
}
