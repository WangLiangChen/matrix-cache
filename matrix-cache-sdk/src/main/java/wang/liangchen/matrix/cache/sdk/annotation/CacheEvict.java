package wang.liangchen.matrix.cache.sdk.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author Liangchen.Wang 2022-07-27 19:19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

    @AliasFor("cacheNames")
    String[] value() default {};

    @AliasFor("value")
    String[] cacheNames() default {};


    String key() default "";


    String keyGenerator() default "";


    String cacheManager() default "";


    String cacheResolver() default "";


    String condition() default "";


    boolean allEntries() default false;


    boolean beforeInvocation() default false;

}