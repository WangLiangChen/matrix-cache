package wang.liangchen.matrix.easycache.sdk.cache.caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.easycache.sdk.override.Cache;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author LiangChen.Wang
 */
public class CaffeineCache extends org.springframework.cache.caffeine.CaffeineCache implements Cache {
    private final static Logger logger = LoggerFactory.getLogger(CaffeineCache.class);
    /**
     * time to live - ttl
     * time to idle - tti
     */
    private final Duration ttl;
    private final Set<Object> keys = new HashSet<>();

    public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache, Duration ttl) {
        this(name, cache, true, ttl);
    }

    public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache, boolean allowNullValues, Duration ttl) {
        super(name, cache, allowNullValues);
        this.ttl = ttl;
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
        keys.clear();
    }

    @Override
    public Set<Object> keys() {
        return this.keys;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.keys.contains(key);
    }

    @Override
    public Duration getTtl() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return "CaffeineCache{" +
                "name='" + getName() + '\'' +
                ", ttl=" + ttl +
                ", allowNullValues=" + isAllowNullValues() +
                '}';
    }
}
