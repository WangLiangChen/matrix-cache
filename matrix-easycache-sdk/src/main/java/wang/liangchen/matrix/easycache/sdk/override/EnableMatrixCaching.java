package wang.liangchen.matrix.easycache.sdk.override;

import org.springframework.cache.annotation.CachingConfigurationSelector;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Liangchen.Wang 2022-07-05 8:13
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableMatrixCaching.MatrixCacheImportSelector.class)
public @interface EnableMatrixCaching {
    class MatrixCacheImportSelector implements ImportSelector {

        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{MatrixCachingConfiguration.class.getName()};
        }
    }

}