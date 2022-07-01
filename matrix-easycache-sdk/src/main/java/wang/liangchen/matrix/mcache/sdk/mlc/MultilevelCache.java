package wang.liangchen.matrix.mcache.sdk.mlc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.framework.commons.json.JsonUtil;
import wang.liangchen.matrix.framework.commons.lock.LocalLockUtil;
import wang.liangchen.matrix.framework.commons.lock.LockReader;
import wang.liangchen.matrix.framework.commons.object.ObjectUtil;
import wang.liangchen.matrix.mcache.sdk.override.Cache;
import wang.liangchen.matrix.mcache.sdk.redis.CacheMessage;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


/**
 * 根据Cache实现的不同 实现Key级别的过期
 *
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCache implements Cache {
    private final Logger logger = LoggerFactory.getLogger(MultilevelCache.class);
    private final String name;
    private final long ttl;
    private final Cache localCache;
    private final Cache distributedCache;
    private final MultilevelCacheManager multilevelCacheManager;

    public MultilevelCache(String name, long ttl, MultilevelCacheManager multilevelCacheManager) {
        this.name = name;
        this.ttl = ttl;
        this.multilevelCacheManager = multilevelCacheManager;
        this.localCache = multilevelCacheManager.getLocalCache(name, ttl);
        this.distributedCache = multilevelCacheManager.getDistributedCache(name, ttl);
    }

    @Override
    public ValueWrapper get(Object key) {
        logger.debug(loggerPrefix("get,local", key));
        // null说明缓存不存在
        ValueWrapper valueWrapper = localCache.get(key);
        if (null != valueWrapper) {
            logger.debug(loggerPrefix("get,local,hit", key, "value"), valueWrapper.get().toString());
            return valueWrapper;
        }
        logger.debug(loggerPrefix("get,local,miss,remote", key));
        valueWrapper = distributedCache.get(key);
        if (null == valueWrapper) {
            logger.debug(loggerPrefix("get,remote,miss", key, "value"), valueWrapper.get().toString());
            return null;
        }
        Object value = valueWrapper.get();
        logger.debug(loggerPrefix("get,remote,hit,>local", key, "value"), valueWrapper.get().toString());
        // 写入localCache
        localCache.put(key, value);
        return valueWrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        logger.debug(loggerPrefix("getWithType", key, "type"), type.getName());
        ValueWrapper valueWrapper = get(key);
        if (null == valueWrapper) {
            return null;
        }
        return ObjectUtil.INSTANCE.cast(valueWrapper.get());
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        logger.debug(loggerPrefix("getWithValueLoader", key));
        distributedCache.get(key, valueLoader);
        return localCache.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        logger.debug(loggerPrefix("put", key, "value"), value.toString());
        distributedCache.put(key, value);
        localCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        logger.debug(loggerPrefix("evict", key));
        distributedCache.evict(key);
        localCache.evict(key);
        // 通过redis发送消息
        multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.evict, key));
    }

    public void evictLocal(Object key) {
        logger.debug(loggerPrefix("evictLocal", key));
        localCache.evict(key);
    }

    @Override
    public void clear() {
        logger.debug(loggerPrefix("clear", ""));
        distributedCache.clear();
        localCache.clear();
        // 发送消息
        multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.clear));
    }

    public void clearLocal() {
        logger.debug(loggerPrefix("clearLocal", ""));
        localCache.clear();
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
    public long getTtl() {
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

    private String loggerPrefix(String action, Object key, String... args) {
        String loggerPrefix = String.format("MultilevelCache(name:%s,ttl:%d,action:%s,key:%s)", name, ttl, action, key.toString());
        String suffix = Arrays.stream(args).map(e -> String.format("%s:{}", e)).collect(Collectors.joining(","));
        return String.format("%s - Args(%s)", loggerPrefix, suffix);
    }

    @Override
    public String toString() {
        return "MultilevelCache{" +
                "name='" + name + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
