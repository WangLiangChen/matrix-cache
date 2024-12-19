package wang.liangchen.matrix.cache.sdk.cache.caffeine;

import wang.liangchen.matrix.cache.sdk.cache.MatrixCache;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author LiangChen.Wang
 */
public class MatrixCaffeineMatrixCache extends org.springframework.cache.caffeine.CaffeineCache implements MatrixCache {
    /**
     * time to live - ttl
     * time to idle - tti
     */
    private final Duration ttl;
    private final Set<Object> keys = new CopyOnWriteArraySet<>();

    public MatrixCaffeineMatrixCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache, boolean allowNullValues, Duration ttl, MatrixCaffeineRemovalListener removalListener) {
        super(name, nativeCache, allowNullValues);
        this.ttl = ttl;
        removalListener.registerDelegate((key, value, cause) -> {
            this.keys.remove(key);
        });
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return super.get(key, () -> {
            T call = valueLoader.call();
            // 此时为同步调用，返回后会设置缓存，所以这里需要添加key
            this.keys.add(key);
            return call;
        });
    }

    @Override
    public void put(Object key, Object value) {
        super.put(key, value);
        keys.add(key);
    }

    @Override
    public void evict(Object key) {
        super.evict(key);
        keys.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        keys.clear();
    }

    @Override
    public Set<Object> keys() {
        return this.keys;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.keys.contains(key);
    }

    @Override
    public Duration getTtl() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return "MatrixCaffeineCache{" +
                "name='" + getName() + '\'' +
                ", ttl=" + ttl +
                ", allowNullValues=" + isAllowNullValues() +
                '}';
    }
}
