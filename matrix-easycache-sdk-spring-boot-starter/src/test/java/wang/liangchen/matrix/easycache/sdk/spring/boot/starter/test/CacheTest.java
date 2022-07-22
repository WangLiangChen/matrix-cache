package wang.liangchen.matrix.easycache.sdk.spring.boot.starter.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;
import wang.liangchen.matrix.easycache.sdk.override.EnableEasyCaching;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Liangchen.Wang 2022-07-22 12:19
 */
@SpringBootTest
@EnableEasyCaching
public class CacheTest {
    @Autowired
    private CacheManager cacheManager;

    @Test
    public void cache() throws InterruptedException {
        wang.liangchen.matrix.easycache.sdk.cache.CacheManager cacheManager1 = (wang.liangchen.matrix.easycache.sdk.cache.CacheManager) cacheManager;
        Cache c = cacheManager1.getCache("wanglc", Duration.ofMinutes(10));
        c.put("name","hello");
        c.clear();
        TimeUnit.SECONDS.sleep(5);
    }
}
