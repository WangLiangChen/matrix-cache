package wang.liangchen.matrix.easycache.sdk.test;


import org.springframework.stereotype.Component;
import wang.liangchen.matrix.easycache.sdk.annotation.Cacheable;

@Component
public class CacheComponent {

    @Cacheable(cacheNames = "cacheable", ttlMs = 100000)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "springCacheable")
    public String cacheable() {
        return "1000";
    }
}
