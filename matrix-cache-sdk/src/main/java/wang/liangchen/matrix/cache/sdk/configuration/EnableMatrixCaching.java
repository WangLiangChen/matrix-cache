package wang.liangchen.matrix.cache.sdk.configuration;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableMatrixCaching.MatrixCachingConfigurationSelector.class)
public @interface EnableMatrixCaching {

    class MatrixCachingConfigurationSelector implements ImportSelector{
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            ClassLoader classLoader = this.getClass().getClassLoader();
            boolean hasCaffeine = ClassUtils.isPresent("com.github.benmanes.caffeine.cache.Caffeine", classLoader);
            boolean hasRedis = ClassUtils.isPresent("org.springframework.data.redis.core.RedisTemplate", classLoader);

            if (hasCaffeine && !hasRedis) {
                return new String[]{MatrixLocalCachingConfiguration.class.getName()};
            }
            if (hasRedis && !hasCaffeine) {
                return new String[]{MatrixDistributedCachingConfiguration.class.getName()};
            }
            if (hasCaffeine && hasRedis) {
                return new String[]{MatrixMultiLevelCachingConfiguration.class.getName()};
            }
            return new String[0];
        }
    }
}
