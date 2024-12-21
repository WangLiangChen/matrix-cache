package wang.liangchen.matrix.cache.sdk.configuration;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import wang.liangchen.matrix.cache.sdk.cache.caffeine.MatrixCaffeineMatrixCacheManager;

public class MatrixLocalCachingConfiguration {
    @Bean
    @Primary
    public CacheManager localCacheManager(CacheProperties cacheProperties,
                                     ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoaderProvider) {
        MatrixCaffeineMatrixCacheManager cacheManager = new MatrixCaffeineMatrixCacheManager();
        caffeineCacheLoaderProvider.ifAvailable(cacheManager::setCacheLoader);
        String specification = cacheProperties.getCaffeine().getSpec();
        if (null == specification || specification.isBlank()) {
            return cacheManager;
        }
        cacheManager.setCacheSpecification(specification);
        cacheManager.setAllowNullValues(true);
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }
}
