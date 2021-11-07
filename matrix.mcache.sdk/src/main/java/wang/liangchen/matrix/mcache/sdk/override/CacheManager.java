package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.lang.Nullable;

/**
 * @author LiangChen.Wang 2021/3/22
 * 覆写CacheManager，扩展获取Cache时设置ttl
 */
public interface CacheManager extends org.springframework.cache.CacheManager {
    @Nullable
    Cache getCache(String cacheName, long ttl);
}
