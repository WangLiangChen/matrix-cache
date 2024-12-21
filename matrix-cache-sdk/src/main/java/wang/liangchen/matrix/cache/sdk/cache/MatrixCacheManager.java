package wang.liangchen.matrix.cache.sdk.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/3/22
 * 覆写CacheManager，扩展获取Cache时设置ttl
 */
public interface MatrixCacheManager extends CacheManager {
    @Nullable
    Cache getCache(String name, Duration ttl);
}
