package wang.liangchen.matrix.mcache.sdk.mlc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.mcache.sdk.redis.CacheMessage;
import wang.liangchen.matrix.framework.commons.json.JsonUtil;
import wang.liangchen.matrix.framework.commons.lock.LocalLockUtil;
import wang.liangchen.matrix.framework.commons.lock.LockReader;
import wang.liangchen.matrix.framework.commons.object.ObjectUtil;
import wang.liangchen.matrix.mcache.sdk.override.Cache;

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
    private final String LOCK_KEY = "MultilevelCache";
    private final String loggerPrefix;
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
        this.loggerPrefix = String.format("MultilevelCache(name:%s,ttl:%d)", name,ttl);
        logger.debug("Construct {}", this);
    }

    @Override
    public ValueWrapper get(Object key) {
        logger.debug(loggerPrefix("get", "key"), key);
        // null说明缓存不存在
        ValueWrapper valueWrapper = localCache.get(key);
        logger.debug(loggerPrefix("getFromLocal", "key", "valueWrapper", "value"), key, valueWrapper, null == valueWrapper ? null : JsonUtil.INSTANCE.toJsonString(valueWrapper.get()));
        if (null != valueWrapper) {
            return valueWrapper;
        }
        valueWrapper = distributedCache.get(key);
        if (null == valueWrapper) {
            logger.debug(loggerPrefix("getFromRemote", "key", "valueWrapper"), key, null);
            return null;
        }
        Object value = valueWrapper.get();
        logger.debug(loggerPrefix("getFromRemote,putIntoLocal", "key", "value"), key, JsonUtil.INSTANCE.toJsonString(value));
        // 写入localCache
        localCache.put(key, value);
        return valueWrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        logger.debug(loggerPrefix("get", "key", "type"), key, type);
        // null说明缓存不存在
        ValueWrapper valueWrapper = localCache.get(key);
        logger.debug(loggerPrefix("getFromLocal", "key", "type", "valueWrapper", "value"), key, type, valueWrapper, null == valueWrapper ? null : JsonUtil.INSTANCE.toJsonString(valueWrapper.get()));
        if (null != valueWrapper) {
            return localCache.get(key, type);
        }
        valueWrapper = distributedCache.get(key);
        if (null == valueWrapper) {
            logger.debug(loggerPrefix("getFromRemote", "key", "type", "valueWrapper"), key, type, null);
            return null;
        }
        T value = distributedCache.get(key, type);
        logger.debug(loggerPrefix("getFromRemote,putIntoLocal", "key", "type", "value"), key, type, JsonUtil.INSTANCE.toJsonString(value));
        // 写入localCache
        localCache.put(key, value);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        logger.debug(loggerPrefix("syncGet", "key", "valueLoader"), key, valueLoader);
        String lockKey = String.format("%s::%s::%s", LOCK_KEY, this.name, key);
        return LocalLockUtil.INSTANCE.executeInReadWriteLock( () -> {
            ValueWrapper valueWrapper = this.get(key);
            if (null == valueWrapper) {
                logger.debug(loggerPrefix("syncGet with readLock", "key", "valueWrapper"), key, null);
                return null;
            }
            Object value = valueWrapper.get();
            logger.debug(loggerPrefix("syncGet with readLock", "key", "value"), key, JsonUtil.INSTANCE.toJsonString(value));
            return new LockReader.LockValueWrapper<>(ObjectUtil.INSTANCE.cast(value));
        }, () -> {
            try {
                T value = valueLoader.call();
                logger.debug(loggerPrefix("syncGet with writeLock", "key", "value"), key, JsonUtil.INSTANCE.toJsonString(value));
                this.put(key, value);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        });
    }

    @Override
    public void put(Object key, Object value) {
        logger.debug(loggerPrefix("put", "key", "value"), key, JsonUtil.INSTANCE.toJsonString(value));
        distributedCache.put(key, value);
        localCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        logger.debug(loggerPrefix("evict", "key"), key);
        distributedCache.evict(key);
        // 发送消息
        multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.evict, key));
    }

    public void evictLocal(Object key) {
        logger.debug(loggerPrefix("evictLocal", "key"), key);
        localCache.evict(key);
    }

    @Override
    public void clear() {
        logger.debug(loggerPrefix("clear"));
        localCache.clear();
        distributedCache.clear();
        // 发送消息
        multilevelCacheManager.sendCacheMessage(CacheMessage.newInstance(this.name, CacheMessage.Action.clear));
    }

    public void clearLocal() {
        logger.debug(loggerPrefix("clearLocal"));
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

    private String loggerPrefix(String method, String... args) {
        String suffix = Arrays.stream(args).map(e -> String.format("%s:{}", e)).collect(Collectors.joining(","));
        if (null == suffix || suffix.length() == 0) {
            return String.format("%s\r\n - Method(name:%s)", loggerPrefix, method);
        }
        return String.format("%s\r\n - Method(name:%s,%s)", loggerPrefix, method, suffix);
    }

    @Override
    public String toString() {
        return "MultilevelCache{" +
                "name='" + name + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
