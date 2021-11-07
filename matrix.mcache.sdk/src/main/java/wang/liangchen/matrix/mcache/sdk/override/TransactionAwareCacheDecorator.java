package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author LiangChen.Wang 2021/4/20
 */
public class TransactionAwareCacheDecorator implements Cache {
    private final Cache targetCache;


    /**
     * Create a new TransactionAwareCache for the given target Cache.
     *
     * @param targetCache the target Cache to decorate
     */
    public TransactionAwareCacheDecorator(Cache targetCache) {
        Assert.notNull(targetCache, "Target Cache must not be null");
        this.targetCache = targetCache;
    }


    /**
     * Return the target Cache that this Cache should delegate to.
     */
    public Cache getTargetCache() {
        return this.targetCache;
    }

    @Override
    public String getName() {
        return this.targetCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return this.targetCache.getNativeCache();
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        return this.targetCache.get(key);
    }

    @Override
    public <T> T get(Object key, @Nullable Class<T> type) {
        return this.targetCache.get(key, type);
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        return this.targetCache.get(key, valueLoader);
    }

    @Override
    public void put(final Object key, @Nullable final Object value) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionAwareCacheDecorator.this.targetCache.put(key, value);
                }
            });
        } else {
            this.targetCache.put(key, value);
        }
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        return this.targetCache.putIfAbsent(key, value);
    }

    @Override
    public void evict(final Object key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionAwareCacheDecorator.this.targetCache.evict(key);
                }
            });
        } else {
            this.targetCache.evict(key);
        }
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return this.targetCache.evictIfPresent(key);
    }

    @Override
    public void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    targetCache.clear();
                }
            });
        } else {
            this.targetCache.clear();
        }
    }

    @Override
    public boolean invalidate() {
        return this.targetCache.invalidate();
    }

    @Override
    public Set<Object> keys() {
        return targetCache.keys();
    }

    @Override
    public boolean containsKey(Object key) {
        return targetCache.containsKey(key);
    }

    @Override
    public long getTtl() {
        return targetCache.getTtl();
    }
}
