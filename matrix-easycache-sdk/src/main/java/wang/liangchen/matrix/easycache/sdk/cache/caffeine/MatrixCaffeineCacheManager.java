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
public class MatrixCaffeineCacheManager extends AbstractCacheManager {
    private final Caffeine<Object, Object> cacheBuilder;
    @Nullable
    private final CacheLoader<Object, Object> cacheLoader;

    public MatrixCaffeineCacheManager(Caffeine<Object, Object> cacheBuilder, @Nullable CacheLoader<Object, Object> cacheLoader) {
        this.cacheBuilder = cacheBuilder;
        this.cacheLoader = cacheLoader;
    }

    public MatrixCaffeineCacheManager(Caffeine<Object, Object> cacheBuilder) {
        this.cacheBuilder = cacheBuilder;
        this.cacheLoader = null;
    }


    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        return caffeineCache(name, ttl);
    }

    private MatrixCaffeineCache caffeineCache(String name, Duration ttl) {
        return new MatrixCaffeineCache(name, nativeCache(name, ttl), isAllowNullValues(), ttl);
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(String name, Duration ttl) {
        Caffeine<Object, Object> innerCacheBuilder = this.cacheBuilder;
        if (Duration.ZERO.compareTo(ttl) < 0) {
            innerCacheBuilder = Caffeine.newBuilder();
            innerCacheBuilder.expireAfterWrite(ttl);
        }
        return (this.cacheLoader != null ? innerCacheBuilder.build(this.cacheLoader) : innerCacheBuilder.build());
    }
}
