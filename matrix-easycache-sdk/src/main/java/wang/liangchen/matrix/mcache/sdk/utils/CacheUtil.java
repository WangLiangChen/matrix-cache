package wang.liangchen.matrix.mcache.sdk.utils;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import wang.liangchen.matrix.framework.springboot.context.BeanLoader;

/**
 * @author LiangChen.Wang 2021/5/26
 */
public enum CacheUtil {
    // instance
    INSTANCE;

    public CacheManager cacheManager() {
        return BeanLoader.INSTANCE.getBean("cacheManager");
    }

    public Cache cache(String cacheName) {
        return cacheManager().getCache(cacheName);
    }

    public Cache cache(String cacheName, long ttl) {
        CacheManager cacheManager = cacheManager();
        if (cacheManager instanceof CacheManager) {
            return ((wang.liangchen.matrix.mcache.sdk.override.CacheManager) cacheManager).getCache(cacheName, ttl);
        }
        throw new RuntimeException("cacheManager must be liangchen.wang.gradf.framework.cache.override.CacheManager");
    }

}
