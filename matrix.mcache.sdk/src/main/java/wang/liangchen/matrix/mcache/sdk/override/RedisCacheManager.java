package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import wang.liangchen.matrix.mcache.sdk.redis.RedisCache;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author LiangChen.Wang 2021/4/16
 */
public class RedisCacheManager extends AbstractCacheManager {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration defaultCacheConfig;
    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration;
    private final boolean allowInFlightCacheCreation;
    private boolean allowNullValues = true;

    private RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, boolean allowInFlightCacheCreation) {
        Assert.notNull(cacheWriter, "redisTemplate must not be null!");
        Assert.notNull(cacheWriter, "CacheWriter must not be null!");
        Assert.notNull(defaultCacheConfiguration, "DefaultCacheConfiguration must not be null!");
        this.redisTemplate = redisTemplate;
        this.cacheWriter = cacheWriter;
        this.defaultCacheConfig = defaultCacheConfiguration;
        this.initialCacheConfiguration = new LinkedHashMap<>();
        this.allowInFlightCacheCreation = allowInFlightCacheCreation;
    }

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        this(redisTemplate, cacheWriter, defaultCacheConfiguration, true);
    }

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                             String... initialCacheNames) {

        this(redisTemplate, cacheWriter, defaultCacheConfiguration, true, initialCacheNames);
    }

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                             boolean allowInFlightCacheCreation, String... initialCacheNames) {

        this(redisTemplate, cacheWriter, defaultCacheConfiguration, allowInFlightCacheCreation);
        for (String cacheName : initialCacheNames) {
            this.initialCacheConfiguration.put(cacheName, defaultCacheConfiguration);
        }
    }

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                             Map<String, RedisCacheConfiguration> initialCacheConfigurations) {

        this(redisTemplate, cacheWriter, defaultCacheConfiguration, initialCacheConfigurations, true);
    }

    public RedisCacheManager(RedisTemplate<Object, Object> redisTemplate, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                             Map<String, RedisCacheConfiguration> initialCacheConfigurations, boolean allowInFlightCacheCreation) {

        this(redisTemplate, cacheWriter, defaultCacheConfiguration, allowInFlightCacheCreation);

        Assert.notNull(initialCacheConfigurations, "InitialCacheConfigurations must not be null!");

        this.initialCacheConfiguration.putAll(initialCacheConfigurations);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();
        for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
            caches.add(createRedisCache(entry.getKey(), 0L, allowNullValues, entry.getValue()));
        }
        return caches;
    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, long ttl) {
        return allowInFlightCacheCreation ? createRedisCache(name, ttl, allowNullValues, defaultCacheConfig) : null;
    }

    protected RedisCache createRedisCache(String name, long ttl, boolean allowNullValues, @Nullable RedisCacheConfiguration cacheConfig) {
        return new RedisCache(name, ttl, allowNullValues, cacheWriter, cacheConfig != null ? cacheConfig : defaultCacheConfig, redisTemplate);
    }
}
