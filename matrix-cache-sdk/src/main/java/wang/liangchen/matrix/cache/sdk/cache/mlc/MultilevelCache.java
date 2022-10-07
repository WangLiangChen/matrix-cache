package wang.liangchen.matrix.cache.sdk.cache.mlc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 根据Cache实现的不同 实现Key级别的过期
 *
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCache implements wang.liangchen.matrix.cache.sdk.cache.Cache {
    private final Logger logger = LoggerFactory.getLogger(MultilevelCache.class);
    private final String name;
    private final Duration ttl;
    private final Cache localCache;
    private final Cache distributedCache;
    private final MultilevelCacheManager multilevelCacheManager;

    public MultilevelCache(String name, Duration ttl, MultilevelCacheManager multilevelCacheManager) {
        this.name = name;
        this.ttl = ttl;
        this.multilevelCacheManager = multilevelCacheManager;
        this.localCache = multilevelCacheManager.getLocalCache(name, ttl);
        this.distributedCache = multilevelCacheManager.getDistributedCache(name, ttl);
    }

    @Override
    public ValueWrapper get(Object key) {
        // null说明缓存不存在
        ValueWrapper valueWrapper = localCache.get(key);
        if (null != valueWrapper) {
            logger.debug("LocalCache Hit,name: {},key: {}", this.name, key);
            return valueWrapper;
        }
        logger.debug("LocalCache Miss,name: {},key: {}", this.name, key);
        valueWrapper = distributedCache.get(key);
        if (null == valueWrapper) {
            logger.debug("DistributedCache Miss,name: {},key: {}", this.name, key);
            return null;
        }
        logger.debug("DistributedCache Hit,name: {},key: {}", this.name, key);
        Object value = valueWrapper.get();
        // 写入localCache
        localCache.put(key, value);
        logger.debug("Sync to LocalCache,name: {},key: {}", this.name, key);
        return valueWrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = localCache.get(key, type);
        if (null != value) {
            logger.debug("LocalCache Hit,name: {},key: {}", this.name, key);
            return value;
        }
        logger.debug("LocalCache Miss,name: {},key: {}", this.name, key);
        value = distributedCache.get(key, type);
        if (null == value) {
            logger.debug("DistributedCache Miss,name: {},key: {}", this.name, key);
            return null;
        }
        logger.debug("DistributedCache Hit,name: {},key: {}", this.name, key);
        localCache.put(key, value);
        logger.debug("Sync to LocalCache,name: {},key: {}", this.name, key);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        boolean hits[] = {true, true};
        T value = localCache.get(key, () -> {
            hits[0] = false;
            logger.debug("LocalCache Miss,name: {},key: {}", this.name, key);
            return distributedCache.get(key, () -> {
                hits[1] = false;
                logger.debug("DistributedCache Miss,name: {},key: {}", this.name, key);
                return valueLoader.call();
            });
        });
        if (hits[0]) {
            logger.debug("LocalCache Hit,name: {},key: {}", this.name, key);
        }
        if (!hits[0] && hits[1]) {
            logger.debug("DistributedCache Hit,name: {},key: {}", this.name, key);
        }
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        distributedCache.put(key, value);
        logger.debug("put DistributedCache,name: {},key: {}", this.name, key);
        localCache.put(key, value);
        logger.debug("put LocalCache,name: {},key: {}", this.name, key);
    }

    @Override
    public void evict(Object key) {
        distributedCache.evict(key);
        logger.debug("evict DistributedCache,name: {},key: {}", this.name, key);
        localCache.evict(key);
        logger.debug("evict LocalCache,name: {},key: {}", this.name, key);
    }

    @Override
    public void clear() {
        distributedCache.clear();
        logger.debug("clear DistributedCache,name: {}", this.name);
        localCache.clear();
        logger.debug("clear LocalCache,name: {}", this.name);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        distributedCache.putIfAbsent(key, value);
        logger.debug("putIfAbsent DistributedCache,name: {},key: {}", this.name, key);
        ValueWrapper valueWrapper = localCache.putIfAbsent(key, value);
        logger.debug("putIfAbsent LocalCache,name: {},key: {}", this.name, key);
        return valueWrapper;
    }

    @Override
    public boolean evictIfPresent(Object key) {
        distributedCache.evictIfPresent(key);
        logger.debug("evictIfPresent DistributedCache,name: {},key: {}", this.name, key);
        boolean evictIfPresent = localCache.evictIfPresent(key);
        logger.debug("evictIfPresent LocalCache,name: {},key: {}", this.name, key);
        return evictIfPresent;
    }

    @Override
    public boolean invalidate() {
        distributedCache.invalidate();
        return localCache.invalidate();
    }

    @Override
    public Set<Object> keys() {
        if (localCache instanceof wang.liangchen.matrix.cache.sdk.cache.Cache) {
            return ((wang.liangchen.matrix.cache.sdk.cache.Cache) localCache).keys();
        }
        return Collections.emptySet();
    }

    @Override
    public boolean containsKey(Object key) {
        if (localCache instanceof wang.liangchen.matrix.cache.sdk.cache.Cache) {
            return ((wang.liangchen.matrix.cache.sdk.cache.Cache) localCache).containsKey(key);
        }
        return false;
    }

    @Override
    public Duration getTtl() {
        return this.ttl;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }
}
