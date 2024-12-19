package wang.liangchen.matrix.cache.sdk.configuration;


import com.github.benmanes.caffeine.cache.CacheLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import wang.liangchen.matrix.cache.sdk.cache.CacheLevel;
import wang.liangchen.matrix.cache.sdk.cache.caffeine.MatrixCaffeineMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.mlc.MultilevelMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.MatrixRedisMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.serializer.ProtostuffRedisSerializer;
import wang.liangchen.matrix.cache.sdk.generator.MatrixKeyGenerator;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheAnnotationParser;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheInterceptor;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheResolver;

import java.util.HashMap;
import java.util.Map;

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
    public MatrixCacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource,
                                                           CacheProperties cacheProperties,
                                                           ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoaderProvider,
                                                           ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        MatrixCacheInterceptor matrixCacheInterceptor = new MatrixCacheInterceptor();
        matrixCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        matrixCacheInterceptor.setKeyGenerator(new MatrixKeyGenerator());
        CacheManager cacheManager = resolveCacheManager(cacheProperties, caffeineCacheLoaderProvider, redisConnectionFactoryProvider);
        matrixCacheInterceptor.setCacheManager(cacheManager);
        matrixCacheInterceptor.setCacheResolver(new MatrixCacheResolver(cacheManager));
        return matrixCacheInterceptor;
    }

    private CacheManager resolveCacheManager(CacheProperties cacheProperties, ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoaderProvider, ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        Map<CacheLevel, CacheManager> cacheManagers = new HashMap<>();
        caffeineCacheLoaderProvider.ifAvailable(cacheLoader -> {
            MatrixCaffeineMatrixCacheManager cacheManager = new MatrixCaffeineMatrixCacheManager();
            cacheManager.setCacheLoader(cacheLoader);
            String specification = cacheProperties.getCaffeine().getSpec();
            if (StringUtils.hasText(specification)) {
                cacheManager.setCacheSpecification(specification);
            }
            cacheManagers.put(CacheLevel.LOCAL, cacheManager);
        });
        redisConnectionFactoryProvider.ifAvailable(redisConnectionFactory -> {
            CacheProperties.Redis redisProperties = cacheProperties.getRedis();
            RedisCacheConfiguration defaultRedisCacheConfig = RedisCacheConfiguration.defaultCacheConfig();
            if (redisProperties.getKeyPrefix() != null) {
                defaultRedisCacheConfig = defaultRedisCacheConfig.prefixCacheNameWith(redisProperties.getKeyPrefix());
            }
            if (!redisProperties.isCacheNullValues()) {
                defaultRedisCacheConfig = defaultRedisCacheConfig.disableCachingNullValues();
            }
            if (!redisProperties.isUseKeyPrefix()) {
                defaultRedisCacheConfig = defaultRedisCacheConfig.disableKeyPrefix();
            }
            RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setValueSerializer(new ProtostuffRedisSerializer<>());
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.afterPropertiesSet();
            cacheManagers.put(CacheLevel.DISTRIBUTED, new MatrixRedisMatrixCacheManager(defaultRedisCacheConfig, redisTemplate));
        });
        if (cacheManagers.size() == 1) {
            return cacheManagers.values().iterator().next();
        }
        if (cacheManagers.size() == 2) {
            return new MultilevelMatrixCacheManager((MatrixCaffeineMatrixCacheManager) cacheManagers.get(CacheLevel.LOCAL), (MatrixRedisMatrixCacheManager) cacheManagers.get(CacheLevel.DISTRIBUTED));
        }
        return null;
    }

}
