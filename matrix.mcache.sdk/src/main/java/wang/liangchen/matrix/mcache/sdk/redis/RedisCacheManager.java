package wang.liangchen.matrix.mcache.sdk.redis;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.mcache.sdk.override.AbstractCacheManager;
import wang.liangchen.matrix.mcache.sdk.override.Cache;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author LiangChen.Wang 2021/4/16
 */
public class RedisCacheManager extends AbstractCacheManager {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration defaultCacheConfig;

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, boolean transactionAware, RedisCacheConfiguration defaultCacheConfig, List<String> initialCacheNames) {
        super(defaultCacheConfig.getAllowCacheNullValues(), transactionAware, initialCacheNames);
        this.redisTemplate = redisTemplate;
        this.cacheWriter = cacheWriter;
        this.defaultCacheConfig = defaultCacheConfig;
    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, long ttl) {
        return new RedisCache(name, ttl, this.isAllowNullValues(), cacheWriter, defaultCacheConfig, redisTemplate);
    }
}
