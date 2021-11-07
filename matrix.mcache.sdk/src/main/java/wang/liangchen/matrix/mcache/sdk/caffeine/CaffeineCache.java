package wang.liangchen.matrix.mcache.sdk.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.mcache.sdk.override.Cache;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author LiangChen.Wang
 */
public class CaffeineCache extends org.springframework.cache.caffeine.CaffeineCache implements Cache {
    private final static Logger logger = LoggerFactory.getLogger(CaffeineCache.class);
    /**
     * time to live
     */
    private long ttl;
    /**
     * time to idle
     */
    private long tti;
    private final Set<Object> keys;

    public CaffeineCache(String name, long ttl, boolean allowNullValues, Caffeine<Object, Object> cacheBuilder, CacheLoader<Object, Object> cacheLoader) {
        super(name, CaffeineCacheCreator.INSTANCE.createNativeCache(cacheBuilder, cacheLoader, ttl), allowNullValues);
        this.ttl = ttl;
        this.keys = new CopyOnWriteArraySet<>();
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
    public long getTtl() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return "CaffeineCache{" +
                "name='" + getName() + '\'' +
                ", ttl=" + ttl +
                ", tti=" + tti +
                ", allowNullValues=" + isAllowNullValues() +
                '}';
    }
}
