package wang.liangchen.matrix.mcache.sdk.annotation;

import org.springframework.cache.annotation.CacheEvict;

import java.lang.annotation.*;

/**
 * @author Liangchen.Wang
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Caching {
    Cacheable[] cacheable() default {};

    CachePut[] put() default {};

    CacheEvict[] evict() default {};
}
