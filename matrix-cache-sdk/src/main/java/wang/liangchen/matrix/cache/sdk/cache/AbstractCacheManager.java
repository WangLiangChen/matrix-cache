package wang.liangchen.matrix.cache.sdk.cache;


import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractCacheManager implements CacheManager {
    private final ConcurrentMap<String, org.springframework.cache.Cache> cacheMap = new ConcurrentHashMap<>(32);
    private boolean allowNullValues = true;
    private boolean transactionAware = false;

    @Override
    @Nullable
    public org.springframework.cache.Cache getCache(String name) {
        return getCache(name, Duration.ZERO);
    }

    @Override
    public org.springframework.cache.Cache getCache(String name, Duration ttl) {
        org.springframework.cache.Cache cache = this.cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        return this.cacheMap.computeIfAbsent(name, cacheName -> decorateCache(getMissingCache(name, ttl)));
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheMap.keySet();
    }

    @Nullable
    protected final org.springframework.cache.Cache lookupCache(String name) {
        return this.cacheMap.get(name);
    }


    protected org.springframework.cache.Cache decorateCache(org.springframework.cache.Cache cache) {
        return transactionAware ? new TransactionAwareCacheDecorator(cache) : cache;
    }

    @Nullable
    protected abstract org.springframework.cache.Cache getMissingCache(String name, Duration ttl);

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }

    public void setTransactionAware(boolean transactionAware) {
        this.transactionAware = transactionAware;
    }

    public boolean isTransactionAware() {
        return this.transactionAware;
    }
}
