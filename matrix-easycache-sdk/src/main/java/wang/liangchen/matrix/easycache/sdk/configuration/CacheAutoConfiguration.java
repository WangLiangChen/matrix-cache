package wang.liangchen.matrix.easycache.sdk.configuration;


import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.easycache.sdk.override.CachePutOperation;
import wang.liangchen.matrix.easycache.sdk.override.CacheableOperation;
import wang.liangchen.matrix.easycache.sdk.override.SpringCacheAnnotationParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author LiangChen.Wang 2020/9/23
 */
public class CacheAutoConfiguration {
    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheOperationSource cacheOperationSourceOverride() {
        // EnableCaching->CachingConfigurationSelector->ProxyCachingConfiguration->BeanFactoryCacheOperationSourceAdvisor
        return new AnnotationCacheOperationSource(new SpringCacheAnnotationParser());
    }

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, CacheManager cacheManager) {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheOperationSource(cacheOperationSource);
        cacheInterceptor.setCacheManager(cacheManager);
        cacheInterceptor.setCacheResolver(new SimpleCacheResolver() {
            @Override
            public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
                Collection<String> cacheNames = getCacheNames(context);
                Collection<Cache> caches = new ArrayList<>(cacheNames.size());
                CacheManager resolvedCacheManager = getCacheManager();
                if (!(resolvedCacheManager instanceof wang.liangchen.matrix.easycache.sdk.cache.CacheManager)) {
                    for (String cacheName : cacheNames) {
                        Cache cache = resolvedCacheManager.getCache(cacheName);
                        caches.add(cache);
                    }
                    return caches;
                }
                // 自定义的CacheManager
                wang.liangchen.matrix.easycache.sdk.cache.CacheManager matrixCacheManager = (wang.liangchen.matrix.easycache.sdk.cache.CacheManager) resolvedCacheManager;
                BasicOperation operation = context.getOperation();
                long ttl = 0;
                if (operation instanceof CacheableOperation) {
                    ttl = ((CacheableOperation) operation).getTtl();
                } else if (operation instanceof CachePutOperation) {
                    ttl = ((CachePutOperation) operation).getTtl();
                }
                for (String cacheName : cacheNames) {
                    Cache cache = matrixCacheManager.getCache(cacheName, Duration.ofMillis(ttl));
                    caches.add(cache);
                }
                return caches;
            }
        });
        return cacheInterceptor;
    }


}
