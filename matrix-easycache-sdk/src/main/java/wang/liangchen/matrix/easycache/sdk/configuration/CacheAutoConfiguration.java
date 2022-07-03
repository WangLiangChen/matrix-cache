package wang.liangchen.matrix.easycache.sdk.configuration;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;

import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.easycache.sdk.annotation.OverrideBean;
import wang.liangchen.matrix.easycache.sdk.cache.CacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.caffeine.CaffeineCacheManager;
import wang.liangchen.matrix.easycache.sdk.override.CachePutOperation;
import wang.liangchen.matrix.easycache.sdk.override.CacheableOperation;
import wang.liangchen.matrix.easycache.sdk.override.SpringCacheAnnotationParser;
import wang.liangchen.matrix.easycache.sdk.override.CacheInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author LiangChen.Wang 2020/9/23
 */
public class CacheAutoConfiguration {
    @Bean
    @OverrideBean("cacheManager")
    public org.springframework.cache.CacheManager matrixCacheManager() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        cacheBuilder.expireAfterWrite(Duration.ZERO);
        return new CaffeineCacheManager(cacheBuilder);
    }

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
    public CacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, org.springframework.cache.CacheManager springCacheManager) {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheOperationSource(cacheOperationSource);
        cacheInterceptor.setCacheManager(springCacheManager);
        cacheInterceptor.setCacheResolver(cacheResolver(springCacheManager));
        return cacheInterceptor;
    }

    private CacheResolver cacheResolver(org.springframework.cache.CacheManager springCacheManager) {
        SimpleCacheResolver simpleCacheResolver = new SimpleCacheResolver() {
            @Override
            public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
                Duration ttl = null;
                if (springCacheManager instanceof CacheManager) {
                    BasicOperation operation = context.getOperation();
                    if (operation instanceof CacheableOperation) {
                        ttl = ((CacheableOperation) operation).getTtl();
                    } else if (operation instanceof CachePutOperation) {
                        ttl = ((CachePutOperation) operation).getTtl();
                    }
                }
                Collection<String> cacheNames = getCacheNames(context);
                Collection<Cache> caches = new ArrayList<>(cacheNames.size());
                for (String cacheName : cacheNames) {
                    if (null == ttl || Duration.ZERO == ttl) {
                        Cache cache = springCacheManager.getCache(cacheName);
                        caches.add(cache);
                    } else {
                        Cache cache = ((CacheManager) springCacheManager).getCache(cacheName, ttl);
                        caches.add(cache);
                    }
                }
                return caches;
            }
        };
        simpleCacheResolver.setCacheManager(springCacheManager);
        return simpleCacheResolver;
    }


}
