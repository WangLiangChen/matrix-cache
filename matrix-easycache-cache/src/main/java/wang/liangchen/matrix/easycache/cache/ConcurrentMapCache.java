package wang.liangchen.matrix.easycache.cache;


import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapCache extends AbstractValueAdaptingCache {

    private final String name;

    private final ConcurrentMap<Object, Object> nativeCache;


    public ConcurrentMapCache(String name) {
        this(name, new ConcurrentHashMap<>(256), true);
    }

    public ConcurrentMapCache(String name, boolean allowNullValues) {
        this(name, new ConcurrentHashMap<>(256), allowNullValues);
    }

    protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> nativeCache, boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.nativeCache = nativeCache;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final ConcurrentMap<Object, Object> getNativeCache() {
        return this.nativeCache;
    }

    @Override
    protected Object lookup(Object key) {
        return this.nativeCache.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object storeValue = this.nativeCache.computeIfAbsent(key, k -> {
            try {
                return toStoreValue(valueLoader.call());
            } catch (Throwable ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
        });
        return (T) fromStoreValue(storeValue);
    }

    @Override
    public void put(Object key, Object value) {
        this.nativeCache.put(key, toStoreValue(value));
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object existing = this.nativeCache.putIfAbsent(key, toStoreValue(value));
        return toValueWrapper(existing);
    }

    @Override
    public void evict(Object key) {
        this.nativeCache.remove(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return (this.nativeCache.remove(key) != null);
    }

    @Override
    public void clear() {
        this.nativeCache.clear();
    }

    @Override
    public boolean invalidate() {
        boolean notEmpty = !this.nativeCache.isEmpty();
        this.nativeCache.clear();
        return notEmpty;
    }
}
