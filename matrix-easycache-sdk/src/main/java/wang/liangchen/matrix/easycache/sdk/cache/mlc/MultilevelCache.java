package wang.liangchen.matrix.easycache.sdk.cache.mlc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.easycache.sdk.override.Cache;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 根据Cache实现的不同 实现Key级别的过期
 *
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCache implements Cache {
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
            return valueWrapper;
        }
        valueWrapper = distributedCache.get(key);
        if (null == valueWrapper) {
            return null;
        }
        Object value = valueWrapper.get();
        // 写入localCache
        localCache.put(key, value);
        return valueWrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = localCache.get(key, type);
        if (null != value) {
            return value;
        }
        value = distributedCache.get(key, type);
        if (null == value) {
            return null;
        }
        localCache.put(key, value);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        distributedCache.get(key, valueLoader);
        return localCache.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        distributedCache.put(key, value);
        localCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        distributedCache.evict(key);
        localCache.evict(key);
        // 通过redis发送消息
        // multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.evict, key));
    }

    @Override
    public void clear() {
        distributedCache.clear();
        localCache.clear();
        // 发送消息
        // multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.clear));
    }

    public void evictLocal(Object key) {
        localCache.evict(key);
    }

    public void clearLocal() {
        localCache.clear();
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        distributedCache.putIfAbsent(key, value);
        return localCache.putIfAbsent(key, value);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        distributedCache.evictIfPresent(key);
        return localCache.evictIfPresent(key);
    }

    @Override
    public boolean invalidate() {
        distributedCache.invalidate();
        return localCache.invalidate();
    }

    @Override
    public Set<Object> keys() {
        return localCache.keys();
    }

    @Override
    public boolean containsKey(Object key) {
        return localCache.containsKey(key);
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
