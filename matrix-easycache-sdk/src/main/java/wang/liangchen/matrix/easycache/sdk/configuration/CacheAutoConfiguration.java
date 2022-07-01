package wang.liangchen.matrix.easycache.sdk.configuration;


/**
 * @author LiangChen.Wang 2020/9/23
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import wang.liangchen.matrix.easycache.sdk.override.CachePutOperation;
import wang.liangchen.matrix.easycache.sdk.override.CacheableOperation;
import wang.liangchen.matrix.easycache.sdk.override.SpringCacheAnnotationParser;

import java.util.ArrayList;
import java.util.Collection;

@ConditionalOnBean({CacheAspectSupport.class})
public class CacheAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(CacheAutoConfiguration.class);
    private final String NO_PARAM_KEY = "NO_PARAM";
    private final String NULL_PARAM_KEY = "NULL_PARAM";

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
    public CacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, CachingConfigurer cachingConfigurer) {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheOperationSource(cacheOperationSource);
        cacheInterceptor.configure(cachingConfigurer::errorHandler, cachingConfigurer::keyGenerator, cachingConfigurer::cacheResolver, cachingConfigurer::cacheManager);
        return cacheInterceptor;
    }





    @Bean
    public CachingConfigurer cachingConfigurer(CacheManager cacheManager) {
        return new CachingConfigurer() {
            @Override
            public CacheManager cacheManager() {
                return cacheManager;
            }

            @Override
            public CacheResolver cacheResolver() {
                return new SimpleCacheResolver(cacheManager) {
                    @Override
                    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
                        BasicOperation operation = context.getOperation();
                        long ttl = 0;
                        if (cacheManager instanceof wang.liangchen.matrix.easycache.sdk.override.CacheManager) {
                            if (operation instanceof CacheableOperation) {
                                ttl = ((CacheableOperation) operation).getTtl();
                            } else if (operation instanceof CachePutOperation) {
                                ttl = ((CachePutOperation) operation).getTtl();
                            }
                        }
                        Collection<String> cacheNames = getCacheNames(context);
                        Collection<Cache> result = new ArrayList<>(cacheNames.size());
                        for (String cacheName : cacheNames) {
                            Cache cache;
                            if (ttl == 0) {
                                cache = cacheManager.getCache(cacheName);
                            } else {
                                cache = ((wang.liangchen.matrix.easycache.sdk.override.CacheManager) cacheManager).getCache(cacheName, ttl);
                            }
                            if (cache == null) {
                                throw new IllegalArgumentException("Cannot find cache named '" + cacheName + "' for " + operation);
                            }
                            result.add(cache);
                        }
                        return result;
                    }
                };
            }
        };
    }
}
