package wang.liangchen.matrix.mcache.sdk.caffeine;


import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import wang.liangchen.matrix.framework.commons.string.StringUtil;
import wang.liangchen.matrix.mcache.sdk.override.AbstractCacheManager;
import wang.liangchen.matrix.mcache.sdk.override.Cache;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author LiangChen.Wang 2021/4/15
 */
public class CaffeineCacheManager extends AbstractCacheManager {
    private final CaffeineSpec caffeineSpec;
    private final List<String> initialCacheNames;
    @Nullable
    private final CacheLoader<Object, Object> cacheLoader;

    public CaffeineCacheManager(String caffeineSpec, boolean allowNullValues, boolean transactionAware, List<String> initialCacheNames, CacheLoader<Object, Object> cacheLoader) {
        super(allowNullValues, transactionAware, initialCacheNames);
        if (StringUtil.INSTANCE.isBlank(caffeineSpec)) {
            this.caffeineSpec = null;
        } else {
            this.caffeineSpec = CaffeineSpec.parse(caffeineSpec);
        }
        this.initialCacheNames = initialCacheNames;
        this.cacheLoader = cacheLoader;
    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, long ttl) {
        return new CaffeineCache(name, ttl, this.isAllowNullValues(), null == this.caffeineSpec ? Caffeine.newBuilder() : Caffeine.from(this.caffeineSpec), this.cacheLoader);
    }
}
