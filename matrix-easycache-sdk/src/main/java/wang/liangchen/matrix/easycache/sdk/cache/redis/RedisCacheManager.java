package wang.liangchen.matrix.easycache.sdk.cache.redis;


import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.easycache.sdk.override.AbstractCacheManager;
import wang.liangchen.matrix.easycache.sdk.override.Cache;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/4/16
 */
public class RedisCacheManager extends AbstractCacheManager {
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();

    public RedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, RedisTemplate<Object, Object> redisTemplate) {
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected Cache getMissingCache(String name, Duration ttl) {
        RedisCacheConfiguration localConfig = cacheConfig == null ? defaultCacheConfiguration : cacheConfig;
        if (ttl.compareTo(Duration.ZERO) > 0) {
            localConfig = localConfig.entryTtl(ttl);
        }
        if (!isAllowNullValues()) {
            localConfig = localConfig.disableCachingNullValues();
        }
        return new RedisCache(name, cacheWriter, localConfig, redisTemplate);
    }
}
