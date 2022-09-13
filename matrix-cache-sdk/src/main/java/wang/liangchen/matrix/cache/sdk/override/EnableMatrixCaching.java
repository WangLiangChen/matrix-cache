package wang.liangchen.matrix.cache.sdk.override;

import org.springframework.cache.annotation.EnableCaching;
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
@Import(EnableMatrixCaching.MatrixCacheImportSelector.class)
@EnableCaching
public @interface EnableMatrixCaching {
    class MatrixCacheImportSelector implements ImportSelector {

        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{MatrixCachingConfiguration.class.getName()
                    , MatrixLocalCachingConfiguration.class.getName()
                    , MatrixDistributedCachingConfiguration.class.getName()
                    , MatrixMultilevelCachingConfiguration.class.getName()};
        }
    }

}