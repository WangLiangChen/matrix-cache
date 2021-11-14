package wang.liangchen.matrix.mcache.sdk.configuration;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.*;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import wang.liangchen.matrix.framework.commons.json.JsonUtil;
import wang.liangchen.matrix.framework.commons.secret.HashUtil;
import wang.liangchen.matrix.framework.springboot.annotation.OverrideBean;
import wang.liangchen.matrix.mcache.sdk.caffeine.CaffeineCacheManager;
import wang.liangchen.matrix.mcache.sdk.mlc.MultilevelCacheManager;
import wang.liangchen.matrix.mcache.sdk.override.CachePutOperation;
import wang.liangchen.matrix.mcache.sdk.override.CacheableOperation;
import wang.liangchen.matrix.mcache.sdk.override.*;
import wang.liangchen.matrix.mcache.sdk.redis.RedisCacheCreator;
import wang.liangchen.matrix.mcache.sdk.redis.RedisCacheManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author LiangChen.Wang 2020/9/23
 */

@ConditionalOnClass({CacheManager.class})
@ConditionalOnBean({CacheAspectSupport.class})
@AutoConfigureAfter(org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class)
public class CacheAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(CacheAutoConfiguration.class);
    private final String NO_PARAM_KEY = "NO_PARAM";
    private final String NULL_PARAM_KEY = "NULL_PARAM";

    // ConfigurationClassParser的解析顺序:@PropertySource->@ComponentScan->@Import->@ImportResource->@Bean->接口的默认方法->父类
    //-- caffeine only
    @Bean
    @ConditionalOnBean(org.springframework.cache.caffeine.CaffeineCacheManager.class)
    @ConditionalOnMissingBean(org.springframework.data.redis.cache.RedisCacheManager.class)
    @OverrideBean("cacheManager")
    public CaffeineCacheManager caffeineCacheManagerOverride(CacheProperties cacheProperties, CacheManagerCustomizers customizers, ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
        String specification = cacheProperties.getCaffeine().getSpec();
        String[] initialCacheNames = cacheProperties.getCacheNames().toArray(new String[0]);
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(specification, cacheLoader.getIfAvailable(), initialCacheNames);
        return customizers.customize(cacheManager);
    }

    // -- redis only
    @Bean
    @ConditionalOnBean(org.springframework.data.redis.cache.RedisCacheManager.class)
    @ConditionalOnMissingBean(org.springframework.cache.caffeine.CaffeineCacheManager.class)
    @OverrideBean("cacheManager")
    public RedisCacheManager redisCacheManagerOverride(CacheManagerCustomizers customizers, RedisTemplate<Object, Object> redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate, RedisCacheCreator.INSTANCE.cacheWriter(redisTemplate), RedisCacheConfiguration.defaultCacheConfig());
        return customizers.customize(cacheManager);
    }

    // redis and caffeine
    @Bean
    @ConditionalOnBean(value = {org.springframework.data.redis.cache.RedisCacheManager.class, org.springframework.cache.caffeine.CaffeineCacheManager.class})
    @OverrideBean("cacheManager")
    public MultilevelCacheManager multilevelCacheManager(CacheProperties cacheProperties, CacheManagerCustomizers customizers, ObjectProvider<CacheLoader<Object, Object>> cacheLoader, RedisTemplate<Object, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        //noinspection SpringConfigurationProxyMethods
        CaffeineCacheManager localCacheManager = this.caffeineCacheManagerOverride(cacheProperties, customizers, cacheLoader);
        //noinspection SpringConfigurationProxyMethods
        RedisCacheManager distributedCacheManager = this.redisCacheManagerOverride(customizers, redisTemplate);
        return new MultilevelCacheManager(localCacheManager, distributedCacheManager, stringRedisTemplate);
    }

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheOperationSource cacheOperationSourceOverride() {
//        EnableCaching->CachingConfigurationSelector->ProxyCachingConfiguration
//        ->BeanFactoryCacheOperationSourceAdvisor cacheAdvisor(CacheOperationSource cacheOperationSource, CacheInterceptor cacheInterceptor)
        return new AnnotationCacheOperationSource(new SpringCacheAnnotationParser());
    }

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource,CachingConfigurer cachingConfigurer) {
        CacheInterceptor cacheInterceptor = new wang.liangchen.matrix.mcache.sdk.override.CacheInterceptor();
        cacheInterceptor.setCacheOperationSource(cacheOperationSource);
        cacheInterceptor.configure(cachingConfigurer::errorHandler,cachingConfigurer::keyGenerator,cachingConfigurer::cacheResolver,cachingConfigurer::cacheManager);
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
                        if (cacheManager instanceof wang.liangchen.matrix.mcache.sdk.override.CacheManager) {
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
                                cache = ((wang.liangchen.matrix.mcache.sdk.override.CacheManager) cacheManager).getCache(cacheName, ttl);
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

            @Override
            public KeyGenerator keyGenerator() {
                return (target, method, params) -> {
                    StringBuilder key = new StringBuilder();
                    key.append(target.getClass().getName()).append(".").append(method.getName()).append(":");
                    if (params.length == 0) {
                        key.append(NO_PARAM_KEY);
                        return HashUtil.INSTANCE.md5Digest(key.toString());
                    }
                    Object param;
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) {
                            key.append('-');
                        }
                        param = params[i];
                        if (param == null) {
                            key.append(NULL_PARAM_KEY);
                            continue;
                        }
                        key.append(JsonUtil.INSTANCE.toJsonStringWithTransientField(param));
                    }
                    logger.debug("Generated key:{}", key);
                    return HashUtil.INSTANCE.md5Digest(key.toString());
                };
            }

            @Override
            public CacheErrorHandler errorHandler() {
                return null;
            }
        };
    }
}
