package wang.liangchen.matrix.easycache.sdk.override;


import com.github.benmanes.caffeine.cache.CacheLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.util.StringUtils;
import wang.liangchen.matrix.easycache.sdk.annotation.OverrideBean;
import wang.liangchen.matrix.easycache.sdk.cache.caffeine.MatrixCaffeineCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.mlc.MultilevelCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.redis.MatrixRedisCache;
import wang.liangchen.matrix.easycache.sdk.cache.redis.MatrixRedisCacheManager;

/**
 * @author LiangChen.Wang 2020/9/23
 */
@EnableConfigurationProperties(CacheProperties.class)
class MatrixCachingConfiguration {

    @Bean
    @OverrideBean("cacheManager")
    public MultilevelCacheManager multilevelCacheManager(CacheProperties cacheProperties,
                                                         ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoader,
                                                         ObjectProvider<RedisTemplate<Object, Object>> redisTemplate) {
        MatrixCaffeineCacheManager localCacheManager = createLocalCacheManager(cacheProperties, caffeineCacheLoader);
        RedisTemplate<Object, Object> redisTemplateIfAvailable = redisTemplate.getIfAvailable();
        if (null == redisTemplateIfAvailable) {
            return new MultilevelCacheManager(localCacheManager, null, null);
        }
        MatrixRedisCacheManager distributedCacheManager = createDistributedCacheManager(cacheProperties, redisTemplateIfAvailable);
        return new MultilevelCacheManager(localCacheManager, distributedCacheManager, redisTemplateIfAvailable);
    }

    @Bean
    @ConditionalOnBean(MultilevelCacheManager.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, MultilevelCacheManager multilevelCacheManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        MessageListenerAdapter listener = new MessageListenerAdapter(multilevelCacheManager);
        listener.afterPropertiesSet();
        container.addMessageListener(listener, new ChannelTopic(MatrixRedisCache.EVICT_MESSAGE_TOPIC));
        return container;
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

    private MatrixCaffeineCacheManager createLocalCacheManager(CacheProperties cacheProperties, ObjectProvider<CacheLoader<Object, Object>> cacheLoader) {
        MatrixCaffeineCacheManager cacheManager = new MatrixCaffeineCacheManager();
        cacheLoader.ifAvailable(cacheManager::setCacheLoader);
        String specification = cacheProperties.getCaffeine().getSpec();
        if (StringUtils.hasText(specification)) {
            cacheManager.setCacheSpecification(specification);
        }
        return cacheManager;
    }

    private MatrixRedisCacheManager createDistributedCacheManager(CacheProperties cacheProperties, RedisTemplate<Object, Object> redisTemplate) {
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
        return new MatrixRedisCacheManager(defaultRedisCacheConfig, redisTemplate);
    }

}
