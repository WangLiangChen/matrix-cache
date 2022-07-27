package wang.liangchen.matrix.cache.sdk.cache;

import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/3/22
 * 覆写CacheManager，扩展获取Cache时设置ttl
 */
public interface CacheManager extends org.springframework.cache.CacheManager {
    @Nullable
    org.springframework.cache.Cache getCache(String name, Duration ttl);

    @Nullable
    org.springframework.cache.Cache getCache(String name);
}
