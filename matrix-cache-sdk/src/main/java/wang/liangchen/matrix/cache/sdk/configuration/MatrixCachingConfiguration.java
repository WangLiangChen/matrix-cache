package wang.liangchen.matrix.cache.sdk.configuration;


import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.cache.sdk.generator.MatrixKeyGenerator;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheAnnotationParser;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheInterceptor;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheResolver;

/**
 * @author LiangChen.Wang 2020/9/23
 */
@AutoConfiguration
@EnableMatrixCaching
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class MatrixCachingConfiguration {
    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheOperationSource cacheOperationSourceOverride() {
        // EnableCaching->CachingConfigurationSelector->ProxyCachingConfiguration->BeanFactoryCacheOperationSourceAdvisor
        return new AnnotationCacheOperationSource(new MatrixCacheAnnotationParser());
    }

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MatrixCacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource,
                                                           CacheManager cacheManager) {
        MatrixCacheInterceptor matrixCacheInterceptor = new MatrixCacheInterceptor();
        matrixCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        matrixCacheInterceptor.setKeyGenerator(new MatrixKeyGenerator());
        matrixCacheInterceptor.setCacheManager(cacheManager);
        matrixCacheInterceptor.setCacheResolver(new MatrixCacheResolver(cacheManager));
        return matrixCacheInterceptor;
    }
}
