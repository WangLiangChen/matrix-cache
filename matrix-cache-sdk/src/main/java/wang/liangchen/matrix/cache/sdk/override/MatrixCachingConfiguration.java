package wang.liangchen.matrix.cache.sdk.override;


import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.cache.sdk.generator.MatrixKeyGenerator;

/**
 * @author LiangChen.Wang 2020/9/23
 */
@EnableConfigurationProperties(CacheProperties.class)
class MatrixCachingConfiguration {
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
    public MatrixCacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, CacheManager cacheManager, KeyGenerator keyGenerator) {
        MatrixCacheInterceptor matrixCacheInterceptor = new MatrixCacheInterceptor();
        matrixCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        matrixCacheInterceptor.setCacheManager(cacheManager);
        matrixCacheInterceptor.setKeyGenerator(keyGenerator);
        matrixCacheInterceptor.setCacheResolver(new MatrixCacheResolver(cacheManager));
        return matrixCacheInterceptor;
    }
    
    @Bean
    @ConditionalOnMissingBean(KeyGenerator.class)
    public KeyGenerator keyGenerator() {
        return new MatrixKeyGenerator();
    }
}
