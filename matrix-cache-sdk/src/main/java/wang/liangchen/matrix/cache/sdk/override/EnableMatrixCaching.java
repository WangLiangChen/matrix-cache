package wang.liangchen.matrix.cache.sdk.override;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import wang.liangchen.matrix.cache.sdk.cache.CacheLevel;

import java.lang.annotation.*;

/**
 * @author Liangchen.Wang 2022-07-05 8:13
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableMatrixCaching.MatrixCacheImportSelector.class)
@EnableCaching
public @interface EnableMatrixCaching {
    CacheLevel cacheLevel() default CacheLevel.MULTI;

    class MatrixCacheImportSelector implements ImportSelector {

        @Override
        public String[] selectImports(AnnotationMetadata annotationMetadata) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableMatrixCaching.class.getName(), false));
            ClassLoader classLoader = this.getClass().getClassLoader();
            CacheLevel cacheLevel = (CacheLevel) attributes.get("cacheLevel");
            switch (cacheLevel) {
                case LOCAL:
                    if (ClassUtils.isPresent("com.github.benmanes.caffeine.cache.Caffeine", classLoader)) {
                        return new String[]{MatrixCachingConfiguration.class.getName(), MatrixLocalCachingConfiguration.class.getName()};
                    }
                    throw new RuntimeException("The class 'com.github.benmanes.caffeine.cache.Caffeine' doesn't exist!");
                case DISTRIBUTED:
                    if (ClassUtils.isPresent("org.springframework.data.redis.core.RedisTemplate", classLoader)) {
                        return new String[]{MatrixCachingConfiguration.class.getName(), MatrixDistributedCachingConfiguration.class.getName()};
                    }
                    throw new RuntimeException("The class 'org.springframework.data.redis.core.RedisTemplate' doesn't exist!");
                default:
                    if (ClassUtils.isPresent("com.github.benmanes.caffeine.cache.Caffeine", classLoader)
                            && ClassUtils.isPresent("org.springframework.data.redis.core.RedisTemplate", classLoader)) {
                        return new String[]{MatrixCachingConfiguration.class.getName()
                                , MatrixLocalCachingConfiguration.class.getName()
                                , MatrixDistributedCachingConfiguration.class.getName()
                                , MatrixMultilevelCachingConfiguration.class.getName()};
                    }
                    throw new RuntimeException("The class 'com.github.benmanes.caffeine.cache.Caffeine' or 'org.springframework.data.redis.core.RedisTemplate' doesn't exist!");
            }
        }
    }

}