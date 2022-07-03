package wang.liangchen.matrix.easycache.sdk.test;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import wang.liangchen.matrix.easycache.sdk.cache.Cache;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootTest
public class CacheTest {
    @Resource
    private CacheComponent cacheComponent;
    @Resource
    private CacheManager cacheManager;

    @Test
    public void testCacheable() {
        cacheComponent.cacheable();
        cacheComponent.springCacheable();
        System.out.println();
    }

    @Test
    public void testSpringCacheable() {

    }
}
