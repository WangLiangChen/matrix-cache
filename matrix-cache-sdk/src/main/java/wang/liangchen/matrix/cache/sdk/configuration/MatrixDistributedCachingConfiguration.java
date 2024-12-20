package wang.liangchen.matrix.cache.sdk.configuration;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import wang.liangchen.matrix.cache.sdk.cache.redis.MatrixRedisMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.serializer.ProtostuffRedisSerializer;

public class MatrixDistributedCachingConfiguration {
    @Bean
    @Primary
    public CacheManager cacheManager(CacheProperties cacheProperties,
                                     RedisConnectionFactory redisConnectionFactory) {
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig();
        if (redisProperties.getKeyPrefix() != null) {
            defaultCacheConfig = defaultCacheConfig.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            defaultCacheConfig = defaultCacheConfig.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            defaultCacheConfig = defaultCacheConfig.disableKeyPrefix();
        }

        defaultCacheConfig = defaultCacheConfig.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new ProtostuffRedisSerializer<>()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new ProtostuffRedisSerializer<>()));

        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new ProtostuffRedisSerializer<>());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return new MatrixRedisMatrixCacheManager(defaultCacheConfig, redisTemplate);
    }
}
