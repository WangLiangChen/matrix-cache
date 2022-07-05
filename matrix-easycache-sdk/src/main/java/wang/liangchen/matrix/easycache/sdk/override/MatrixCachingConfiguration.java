package wang.liangchen.matrix.easycache.sdk.override;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.easycache.sdk.annotation.OverrideBean;
import wang.liangchen.matrix.easycache.sdk.cache.caffeine.MatrixCaffeineCacheManager;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2020/9/23
 */
class MatrixCachingConfiguration {
    @Bean
    @OverrideBean("cacheManager")
    public org.springframework.cache.CacheManager matrixCacheManager() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        cacheBuilder.expireAfterWrite(Duration.ZERO);
        return new MatrixCaffeineCacheManager(cacheBuilder);
    }

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
    public MatrixCacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, org.springframework.cache.CacheManager springCacheManager) {
        MatrixCacheInterceptor matrixCacheInterceptor = new MatrixCacheInterceptor();
        matrixCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        matrixCacheInterceptor.setCacheManager(springCacheManager);
        matrixCacheInterceptor.setCacheResolver(new MatrixCacheResolver(springCacheManager));
        return matrixCacheInterceptor;
    }

}
