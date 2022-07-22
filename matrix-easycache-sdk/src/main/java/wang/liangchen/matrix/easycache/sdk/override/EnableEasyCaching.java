package wang.liangchen.matrix.easycache.sdk.override;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.*;

/**
 * @author Liangchen.Wang 2022-07-05 8:13
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableEasyCaching.MatrixCacheImportSelector.class)
public @interface EnableEasyCaching {
    class MatrixCacheImportSelector implements ImportSelector {

        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{MatrixCachingConfiguration.class.getName()};
        }
    }

}