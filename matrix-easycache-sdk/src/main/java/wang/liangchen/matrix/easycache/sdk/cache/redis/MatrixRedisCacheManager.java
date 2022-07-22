package wang.liangchen.matrix.easycache.sdk.cache.redis;


import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.easycache.sdk.cache.AbstractCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/4/16
 */
public class MatrixRedisCacheManager extends AbstractCacheManager {
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();

    public MatrixRedisCacheManager(RedisCacheConfiguration cacheConfig, RedisTemplate<Object, Object> redisTemplate) {
        this.cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisTemplate.getConnectionFactory());
        this.cacheConfig = cacheConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        RedisCacheConfiguration localConfig = cacheConfig == null ? defaultCacheConfiguration : cacheConfig;
        if (Duration.ZERO.compareTo(ttl) < 0) {
            localConfig = localConfig.entryTtl(ttl);
        }
        if (!isAllowNullValues()) {
            localConfig = localConfig.disableCachingNullValues();
        }
        return new MatrixRedisCache(name, cacheWriter, localConfig, redisTemplate);
    }

}
