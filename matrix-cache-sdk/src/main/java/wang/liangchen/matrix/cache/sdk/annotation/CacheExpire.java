package wang.liangchen.matrix.cache.sdk.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author LiangChen.Wang
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheExpire {
    long ttl();

    TimeUnit timeUnit();
}
