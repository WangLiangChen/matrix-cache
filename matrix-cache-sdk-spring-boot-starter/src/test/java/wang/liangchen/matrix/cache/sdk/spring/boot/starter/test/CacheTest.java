package wang.liangchen.matrix.cache.sdk.spring.boot.starter.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import wang.liangchen.matrix.cache.sdk.cache.CacheManager;
import wang.liangchen.matrix.cache.sdk.override.EnableMatrixCaching;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Liangchen.Wang 2022-07-22 12:19
 */
@SpringBootTest
@EnableMatrixCaching
public class CacheTest {
    @Autowired
    private CacheManager cacheManager;

    @Test
    public void cache() throws InterruptedException {
        Cache cache = cacheManager.getCache("TestCache", Duration.ofMinutes(1));
        cache.evict("name_1");
        cache.evict("name_2");
        cache.evict("name_3");
        cache.clear();
        cache.evict("name_4");
        cache.evict("name_5");
        cache.evict("name_6");
        TimeUnit.MINUTES.sleep(3);
    }
}
