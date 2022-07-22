package wang.liangchen.matrix.easycache.sdk.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 扩展补充spring的RedisCache
 *
 * @author LiangChen.Wang
 */
public class MatrixRedisCache extends org.springframework.data.redis.cache.RedisCache implements Cache {
    private final static Logger logger = LoggerFactory.getLogger(MatrixRedisCache.class);
    public static final String EVICT_MESSAGE_TOPIC = "MatrixCacheEvictMessage";
    private final String EMPTY_STRING = "";
    public static final String EVICT_MESSAGE_SPLITTER = "\\";
    private final Duration ttl;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final String keysKey;
    private final BoundSetOperations<Object, Object> keys;
    private final BoundListOperations<Object, Object> evictQueue;

    public MatrixRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, RedisTemplate<Object, Object> redisTemplate) {
        super(name, cacheWriter, cacheConfig);
        this.ttl = cacheConfig.getTtl();
        this.redisTemplate = redisTemplate;

        this.keysKey = this.createCacheKey("keys");
        this.keys = redisTemplate.boundSetOps(keysKey);
        // 有key才能设置expire,所以先add一个
        this.keys.add(EMPTY_STRING);
        this.keys.expire(ttl);

        this.evictQueue = redisTemplate.boundListOps(EVICT_MESSAGE_TOPIC);
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
        // add to evict queue
        evictQueue.rightPush(String.format("%s%s%s", getName(), EVICT_MESSAGE_SPLITTER, key));
        redisTemplate.convertAndSend(EVICT_MESSAGE_TOPIC, EMPTY_STRING);
    }

    @Override
    public void clear() {
        super.clear();
        redisTemplate.delete(this.keysKey);
        redisTemplate.convertAndSend(EVICT_MESSAGE_TOPIC, EMPTY_STRING);
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
