package wang.liangchen.matrix.easycache.sdk.test;


import org.springframework.stereotype.Component;
import wang.liangchen.matrix.easycache.sdk.annotation.Cacheable;

@Component
@org.springframework.cache.annotation.Cacheable("books")
public class CacheComponent {
    public String cacheable() {
        return "1000";
    }
}
