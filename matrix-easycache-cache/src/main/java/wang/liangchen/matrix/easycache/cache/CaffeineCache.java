package wang.liangchen.matrix.easycache.cache;

import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class CaffeineCache extends AbstractValueAdaptingCache {
    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache;

    public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache) {
        this(name, nativeCache, true);
    }

    public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache, boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.nativeCache = nativeCache;
    }


    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
        return this.nativeCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T get(Object key, final Callable<T> valueLoader) {
        Object storeValue = this.nativeCache.get(key, (innerKey) -> {
            try {
                return toStoreValue(valueLoader.call());
            } catch (Exception ex) {
                throw new ValueRetrievalException(innerKey, valueLoader, ex);
            }
        });
        return (T) fromStoreValue(storeValue);
    }

    @Override
    @Nullable
    protected Object lookup(Object key) {
        if (this.nativeCache instanceof LoadingCache) {
            return ((LoadingCache<Object, Object>) this.nativeCache).get(key);
        }
        return this.nativeCache.getIfPresent(key);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        this.nativeCache.put(key, toStoreValue(value));
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
        org.springframework.cache.caffeine.CaffeineCache.PutIfAbsentFunction callable = new org.springframework.cache.caffeine.CaffeineCache.PutIfAbsentFunction(value);
        Object result = this.nativeCache.get(key, callable);
        return (callable.called ? null : toValueWrapper(result));
    }

    @Override
    public void evict(Object key) {
        this.nativeCache.invalidate(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return (this.nativeCache.asMap().remove(key) != null);
    }

    @Override
    public void clear() {
        this.nativeCache.invalidateAll();
    }

    @Override
    public boolean invalidate() {
        boolean notEmpty = !this.nativeCache.asMap().isEmpty();
        this.nativeCache.invalidateAll();
        return notEmpty;
    }


    private class PutIfAbsentFunction implements Function<Object, Object> {

        @Nullable
        private final Object value;

        private boolean called;

        public PutIfAbsentFunction(@Nullable Object value) {
            this.value = value;
        }

        @Override
        public Object apply(Object key) {
            this.called = true;
            return toStoreValue(this.value);
        }
    }


    private class LoadFunction implements Function<Object, Object> {

        private final Callable<?> valueLoader;

        public LoadFunction(Callable<?> valueLoader) {
            this.valueLoader = valueLoader;
        }

        @Override
        public Object apply(Object o) {
            try {
                return toStoreValue(this.valueLoader.call());
            } catch (Exception ex) {
                throw new ValueRetrievalException(o, this.valueLoader, ex);
            }
        }
    }

}

