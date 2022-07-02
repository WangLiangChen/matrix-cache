package wang.liangchen.matrix.easycache.sdk.test;


import org.springframework.stereotype.Component;
import wang.liangchen.matrix.easycache.sdk.annotation.Cacheable;

@Component
public class CacheComponent {

    @Cacheable(cacheNames = "wanglc", ttl = 100)
    public String cacheable() {
        return "";
    }
}
