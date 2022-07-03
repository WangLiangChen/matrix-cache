package wang.liangchen.matrix.easycache.sdk.annotation;

import java.lang.annotation.*;

/**
 * @author Liangchen.Wang
 * 与@Bean同用,可以覆盖同名的bean
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideBean {
    String value();
}
