package wang.liangchen.matrix.cache.sdk.cache.mlc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import wang.liangchen.matrix.cache.sdk.cache.AbstractMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.MatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.caffeine.MatrixCaffeineMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.MatrixRedisMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.consistency.RedisSynchronizer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public class MultiLevelMatrixCacheManager extends AbstractMatrixCacheManager {
    private final static Logger logger = LoggerFactory.getLogger(MultiLevelMatrixCacheManager.class);
    private CacheManager localCacheManager;
    private CacheManager distributedCacheManager;
    private RedisTemplate<Object, Object> redisTemplate;

    @Nullable
    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        return new MultiLevelMatrixCache(name, ttl, this);
    }

    public Cache getLocalCache(String name, Duration ttl) {
        if (null == localCacheManager) {
            return null;
        }
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

    public void setLocalCacheManager(MatrixCaffeineMatrixCacheManager localCacheManager) {
        this.localCacheManager = localCacheManager;
    }

    public void setDistributedCacheManager(MatrixRedisMatrixCacheManager distributedCacheManager) {
        this.distributedCacheManager = distributedCacheManager;
        this.redisTemplate = distributedCacheManager.getRedisTemplate();
        RedisSynchronizer.INSTANCE.init(this);
    }

    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }


    public void handleEvictedKeys(List<Object> messages) {
        messages.stream().map(Object::toString).forEach(message -> {
            int index = message.indexOf(RedisSynchronizer.EVICT_MESSAGE_SPLITTER);
            if (index < 0) {
                Cache cache = localCacheManager.getCache(message);
                Optional.ofNullable(cache).ifPresent(e -> {
                    cache.clear();
                    logger.debug("Cleared cache: {} ", message);
                });
                return;
            }
            String name = message.substring(0, index);
            Cache cache = localCacheManager.getCache(name);
            Optional.ofNullable(cache).ifPresent(e -> {
                String key = message.substring(index + RedisSynchronizer.EVICT_MESSAGE_SPLITTER.length());
                cache.evict(key);
                logger.debug("Evicted cache: {},key: {}", name, key);
            });
        });
    }
}
