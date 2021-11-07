package wang.liangchen.matrix.mcache.sdk.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.framework.commons.enumeration.Symbol;
import wang.liangchen.matrix.mcache.sdk.override.Cache;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 扩展补充spring的RedisCache
 *
 * @author LiangChen.Wang
 */
public class RedisCache extends org.springframework.data.redis.cache.RedisCache implements Cache {
    private final static Logger logger = LoggerFactory.getLogger(RedisCache.class);
    private final long ttl;
    private final BoundSetOperations<Object, Object> keys;

    public RedisCache(String name, long ttl, boolean allowNullValues, RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultRedisCacheConfiguration, RedisTemplate<Object, Object> redisTemplate) {
        super(name, cacheWriter, RedisCacheCreator.INSTANCE.cacheConfig(ttl, allowNullValues, defaultRedisCacheConfiguration));
        this.ttl = ttl;
        String keysKey = this.createCacheKey("keys");
        this.keys = redisTemplate.boundSetOps(keysKey);
        // 有key才能设置expire,所以先add
        this.keys.add(Symbol.BLANK.getSymbol());
        this.keys.expire(ttl, TimeUnit.MILLISECONDS);
        logger.debug("Construct {}", this);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value = super.get(key, valueLoader);
        // 此时为同步调用，返回后会设置缓存，所以这里需要添加key
        this.keys.add(key);
        return value;
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
    }

    @Override
    public void clear() {
        super.clear();
        Set<Object> members = keys.members();
        keys.remove(members.toArray());
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
    public long getTtl() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return "RedisCache{" +
                "name='" + getName() + '\'' +
                ", ttl=" + ttl +
                ", allowNullValues=" + isAllowNullValues() +
                '}';
    }
}
