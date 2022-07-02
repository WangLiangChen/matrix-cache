package wang.liangchen.matrix.easycache.sdk.test;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class CacheTest {
    @Resource
    private CacheComponent cacheComponent;

    @Test
    public void testCacheable() {

    }
}
