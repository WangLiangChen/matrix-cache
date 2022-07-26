package wang.liangchen.matrix.easycache.sdk.cache.mlc;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import wang.liangchen.matrix.easycache.sdk.cache.AbstractCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;
import wang.liangchen.matrix.easycache.sdk.cache.CacheManager;
import wang.liangchen.matrix.easycache.sdk.consistency.CacheSynchronizer;

import java.time.Duration;
import java.util.List;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCacheManager extends AbstractCacheManager {
    private final CacheManager localCacheManager, distributedCacheManager;
    private final RedisTemplate<Object, Object> redisTemplate;

    public MultilevelCacheManager(CacheManager localCacheManager, CacheManager distributedCacheManager,
                                  RedisTemplate<Object, Object> redisTemplate) {
        this.localCacheManager = localCacheManager;
        this.distributedCacheManager = distributedCacheManager;
        this.redisTemplate = redisTemplate;
        CacheSynchronizer.INSTANCE.init(this);
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
        if (null == distributedCacheManager) {
            return null;
        }
        return distributedCacheManager.getCache(name, ttl);
    }

    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void handleMessage(Object message) {
        CacheSynchronizer.INSTANCE.handleMessage();
    }

    public void handleEvictedKeys(List<Object> keys) {
        keys.stream().map(Object::toString).forEach(key -> {
            int index = key.indexOf(CacheSynchronizer.EVICT_MESSAGE_SPLITTER);
        });
    }
}
