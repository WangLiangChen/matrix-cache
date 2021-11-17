package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author LiangChen.Wang 2021/4/20
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(64);
    private final boolean allowNullValues;
    private final boolean transactionAware;
    private final List<String> initialCacheNames;

    public AbstractCacheManager(boolean allowNullValues, boolean transactionAware, List<String> initialCacheNames) {
        this.allowNullValues = allowNullValues;
        this.transactionAware = transactionAware;
        this.initialCacheNames = initialCacheNames;
    }

    @Override
    public void afterPropertiesSet() {
        Collection<? extends Cache> caches = loadCaches();
        this.cacheMap.clear();
        for (Cache cache : caches) {
            String name = cache.getName();
            this.cacheMap.put(name, decorateCache(cache));
        }
    }

    protected Collection<? extends Cache> loadCaches() {
        if (null == this.initialCacheNames) {
            return Collections.emptyList();
        }
        int size = this.initialCacheNames.size();
        if (0 == size) {
            return Collections.emptyList();
        }
        Collection<Cache> caches = new ArrayList<>(initialCacheNames.size());
        for (String initialCacheName : this.initialCacheNames) {
            caches.add(getMissingCache(initialCacheName, 0L));
        }
        return caches;
    }

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

    public boolean isTransactionAware() {
        return this.transactionAware;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    protected Cache decorateCache(Cache cache) {
        return (this.transactionAware ? new TransactionAwareCacheDecorator(cache) : cache);
    }

    @Nullable
    protected abstract Cache getMissingCache(String name, long ttl);
}
