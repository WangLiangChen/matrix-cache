package wang.liangchen.matrix.cache.sdk.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.cache.sdk.cache.MatrixCache;
import wang.liangchen.matrix.cache.sdk.consistency.RedisSynchronizer;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 扩展补充spring的RedisCache
 *
 * @author LiangChen.Wang
 */
public class MatrixRedisMatrixCache extends org.springframework.data.redis.cache.RedisCache implements MatrixCache {
    private static final Logger logger = LoggerFactory.getLogger(MatrixRedisMatrixCache.class);
    private final Duration ttl;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final BoundSetOperations<Object, Object> keys;


    public MatrixRedisMatrixCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, RedisTemplate<Object, Object> redisTemplate) {
        super(name, cacheWriter, cacheConfig);
        this.ttl = cacheConfig.getTtlFunction().getTimeToLive(Object.class, null);
        this.redisTemplate = redisTemplate;

        cacheWriter.get

        String keysKey = this.createCacheKey("keys");
        this.keys = redisTemplate.boundSetOps(keysKey);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T object = super.get(key, () -> {
            T call = valueLoader.call();
            // 此时为同步调用，返回后会设置缓存，所以这里需要添加key
            this.keys.add(key);
            return call;
        });
        return object;
    }

    @Override
    public void put(Object key, Object value) {
        super.put(key, value);
        keys.add(key);
    }

    @Override
    public void evict(Object key) {
        super.evict(key);
        keys.remove(key);
        // add to evict queue
        RedisSynchronizer.INSTANCE.sendEvictMessage(this.getName(), String.valueOf(key));
    }

    @Override
    public void clear() {
        super.clear();
        keys.expire(Duration.ZERO);
        RedisSynchronizer.INSTANCE.sendEvictMessage(this.getName());
    }

    @Override
    public Set<Object> keys() {
        return this.keys.members();
    }

    @Override
    public boolean containsKey(Object key) {
        return keys.isMember(key);
    }

    @Override
    public Duration getTtl() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return "RedisCache{" + "name='" + getName() + '\'' + ", ttl=" + ttl + ", allowNullValues=" + isAllowNullValues() + '}';
    }
}
