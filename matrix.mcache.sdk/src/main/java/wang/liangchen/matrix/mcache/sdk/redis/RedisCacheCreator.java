package wang.liangchen.matrix.mcache.sdk.redis;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import wang.liangchen.matrix.framework.commons.object.ProtostuffUtil;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2020/9/23
 */
public enum RedisCacheCreator {
    //
    INSTANCE;

    public RedisCacheWriter cacheWriter(RedisTemplate redisTemplate) {
        return RedisCacheWriter.lockingRedisCacheWriter(redisTemplate.getConnectionFactory());
    }

    public RedisCacheConfiguration cacheConfig(long ttl, boolean allowNullValues, RedisCacheConfiguration defaultRedisCacheConfiguration) {
        RedisCacheConfiguration redisCacheConfiguration = null == defaultRedisCacheConfiguration ? createDefaultRedisCacheConfiguration(ttl, allowNullValues) : defaultRedisCacheConfiguration;
        redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.ofMillis(ttl));
        if (allowNullValues) {
            return redisCacheConfiguration;
        }
        return redisCacheConfiguration.disableCachingNullValues();
    }

    private RedisCacheConfiguration createDefaultRedisCacheConfiguration(long ttl, boolean allowNullValues) {
        RedisSerializer<String> keySerializer = StringRedisSerializer.UTF_8;
        RedisSerializer<Object> valueSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object object) throws SerializationException {
                return ProtostuffUtil.INSTANCE.object2Bytes(object);
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                return ProtostuffUtil.INSTANCE.bytes2Object(bytes);
            }
        };
        RedisSerializationContext.SerializationPair<String> keyPair = RedisSerializationContext.SerializationPair.fromSerializer(keySerializer);
        RedisSerializationContext.SerializationPair<Object> valuePair = RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);
        return RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(keyPair).serializeValuesWith(valuePair);
    }
}
