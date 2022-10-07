package wang.liangchen.matrix.cache.sdk.override;


import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.cache.sdk.annotation.OverrideBean;
import wang.liangchen.matrix.cache.sdk.cache.redis.MatrixRedisCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.serializer.ProtostuffRedisSerializer;

/**
 * @author LiangChen.Wang 2020/9/23
 */
// @ConditionalOnProperty("spring.redis.host")
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnMissingClass("com.github.benmanes.caffeine.cache.Caffeine")
@EnableConfigurationProperties(CacheProperties.class)
class MatrixDistributedCachingConfiguration {

    @OverrideBean("cacheManager")
    @Bean
    public CacheManager matrixDistributedCacheManager(CacheProperties cacheProperties, RedisTemplate<Object, Object> redisTemplate) {
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

    @Bean
    @ConditionalOnClass(name = {"io.protostuff.ProtostuffIOUtil"})
    @ConditionalOnMissingBean(name = {"redisTemplate"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setValueSerializer(new ProtostuffRedisSerializer<>());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
