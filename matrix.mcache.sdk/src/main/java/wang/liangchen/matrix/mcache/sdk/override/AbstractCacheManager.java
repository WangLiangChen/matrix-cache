package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author LiangChen.Wang 2021/4/20
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {
    private boolean transactionAware = false;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(64);

    @Override
    public void afterPropertiesSet() {
        Collection<? extends Cache> caches = loadCaches();
        this.cacheMap.clear();
        for (Cache cache : caches) {
            String name = cache.getName();
            this.cacheMap.put(name, decorateCache(cache));
        }
    }

    protected abstract Collection<? extends Cache> loadCaches();

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheMap.keySet();
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        return getCache(name, 0L);
    }

    @Override
    @Nullable
    public Cache getCache(String name, long ttl) {
        Cache cache = this.cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        return this.cacheMap.computeIfAbsent(name, key -> {
            Cache missingCache = getMissingCache(name, ttl);
            if (null == missingCache) {
                return null;
            }
            return decorateCache(missingCache);
        });
    }

    @Nullable
    protected final Cache lookupCache(String name) {
        return this.cacheMap.get(name);
    }

    public void setTransactionAware(boolean transactionAware) {
        this.transactionAware = transactionAware;
    }

    public boolean isTransactionAware() {
        return this.transactionAware;
    }


    protected Cache decorateCache(Cache cache) {
        return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
    }

    @Nullable
    protected abstract Cache getMissingCache(String name, long ttl);
}
