package wang.liangchen.matrix.easycache.sdk.spring.boot.starter.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import wang.liangchen.matrix.easycache.sdk.cache.CacheManager;
import wang.liangchen.matrix.easycache.sdk.override.EnableEasyCaching;

import java.time.Duration;

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
        Cache cache = cacheManager.getCache("TestCache", Duration.ofMinutes(1));
        cache.evict("name_1");
        cache.evict("name_2");
        cache.evict("name_3");
        cache.evict("name_4");
        cache.evict("name_5");
    }
}
