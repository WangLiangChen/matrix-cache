package wang.liangchen.matrix.easycache.sdk.test;


import org.springframework.stereotype.Component;
import wang.liangchen.matrix.easycache.sdk.annotation.Cacheable;

@Component
public class CacheComponent {

    @Cacheable(cacheNames = "cacheable", ttlMs = 100000)
    public String cacheable() {
        return "1000";
    }

    @Cacheable(cacheNames = "springCacheable", ttlMs = 200000)
    public String springCacheable() {
        return "2000";
    }
}
