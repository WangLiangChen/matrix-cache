package wang.liangchen.matrix.easycache.sdk.cache.mlc;

import org.springframework.lang.Nullable;
import wang.liangchen.matrix.easycache.sdk.cache.AbstractCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;
import wang.liangchen.matrix.easycache.sdk.cache.CacheManager;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCacheManager extends AbstractCacheManager {
    private final CacheManager localCacheManager, distributedCacheManager;

    public MultilevelCacheManager(CacheManager localCacheManager, CacheManager distributedCacheManager) {
        this.localCacheManager = localCacheManager;
        this.distributedCacheManager = distributedCacheManager;

    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        return new MultilevelCache(name, ttl, this);
    }

    public Cache getLocalCache(String name, Duration ttl) {
        return localCacheManager.getCache(name, ttl);
    }

    public Cache getDistributedCache(String name, Duration ttl) {
        return distributedCacheManager.getCache(name, ttl);
    }
}
