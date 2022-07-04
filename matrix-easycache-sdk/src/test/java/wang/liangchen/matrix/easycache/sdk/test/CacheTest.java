package wang.liangchen.matrix.easycache.sdk.test;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import javax.annotation.Resource;

@SpringBootTest
public class CacheTest {
    @Resource
    private CacheComponent cacheComponent;
    @Resource
    private CacheManager cacheManager;

    @Test
    public void testCacheable() {
        cacheComponent.cacheable();
        System.out.println();
    }

    @Test
    public void testSpringCacheable() {

    }
}
