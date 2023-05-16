package wang.liangchen.matrix.cache.sdk.cache.mlc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import wang.liangchen.matrix.cache.sdk.cache.AbstractMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.MatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.consistency.CacheSynchronizer;

import java.time.Duration;
import java.util.List;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelMatrixCacheManager extends AbstractMatrixCacheManager {
    private final static Logger logger = LoggerFactory.getLogger(MultilevelMatrixCacheManager.class);
    private final CacheManager localCacheManager, distributedCacheManager;
    private final RedisTemplate<Object, Object> redisTemplate;

    public MultilevelMatrixCacheManager(CacheManager localCacheManager, CacheManager distributedCacheManager,
                                        RedisTemplate<Object, Object> redisTemplate) {
        this.localCacheManager = localCacheManager;
        this.distributedCacheManager = distributedCacheManager;
        this.redisTemplate = redisTemplate;
        CacheSynchronizer.INSTANCE.init(this);
    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        return new MultilevelMatrixCache(name, ttl, this);
    }

    public Cache getLocalCache(String name, Duration ttl) {
        if (localCacheManager instanceof MatrixCacheManager) {
            return ((MatrixCacheManager) localCacheManager).getCache(name, ttl);
        }
        return localCacheManager.getCache(name);
    }

    public Cache getDistributedCache(String name, Duration ttl) {
        if (null == distributedCacheManager) {
            return null;
        }
        if (distributedCacheManager instanceof MatrixCacheManager) {
            return ((MatrixCacheManager) distributedCacheManager).getCache(name, ttl);
        }
        return distributedCacheManager.getCache(name);
    }

    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void handleMessage(Object message) {
        logger.debug("Received the cache clearing message published by Redis");
        CacheSynchronizer.INSTANCE.handleMessage();
    }

    public void handleEvictedKeys(List<Object> messages) {
        messages.stream().map(Object::toString).forEach(message -> {
            int index = message.indexOf(CacheSynchronizer.EVICT_MESSAGE_SPLITTER);
            if (index < 0) {
                Cache cache = localCacheManager.getCache(message);
                cache.clear();
                logger.debug("Cleared cache: {} ", message);
                return;
            }
            String name = message.substring(0, index);
            Cache cache = localCacheManager.getCache(name);
            String key = message.substring(index + 1);
            cache.evict(key);
            logger.debug("Evicted cache: {},key: {}", message, key);
        });
    }
}
