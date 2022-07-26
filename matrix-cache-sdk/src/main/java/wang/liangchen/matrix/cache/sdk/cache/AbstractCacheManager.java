package wang.liangchen.matrix.cache.sdk.cache;


import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractCacheManager implements CacheManager {
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(32);
    private boolean allowNullValues = true;

    @Override
    @Nullable
    public Cache getCache(String name) {
        return getCache(name, Duration.ZERO);
    }

    @Override
    public Cache getCache(String name, Duration ttl) {
        Cache cache = this.cacheMap.get(name);
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
    protected final Cache lookupCache(String name) {
        return this.cacheMap.get(name);
    }


    protected Cache decorateCache(Cache cache) {
        return cache;
    }

    @Nullable
    protected abstract Cache getMissingCache(String name, Duration ttl);

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }
}
