package wang.liangchen.matrix.easycache.sdk.cache;


import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractCacheManager implements CacheManager {
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);
    private Collection<String> cacheNames = Collections.emptySet();
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
        return this.cacheMap.computeIfAbsent(name, cacheName -> {
            updateCacheNames(name);
            return decorateCache(getMissingCache(name, ttl));
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    @Nullable
    protected final Cache lookupCache(String name) {
        return this.cacheMap.get(name);
    }

    private void updateCacheNames(String name) {
        Set<String> cacheNames = new LinkedHashSet<>(this.cacheNames);
        cacheNames.add(name);
        this.cacheNames = Collections.unmodifiableSet(cacheNames);
    }

    protected Cache decorateCache(Cache cache) {
        return cache;
    }

    @Nullable
    protected Cache getMissingCache(String name, Duration ttl) {
        return null;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }
}
