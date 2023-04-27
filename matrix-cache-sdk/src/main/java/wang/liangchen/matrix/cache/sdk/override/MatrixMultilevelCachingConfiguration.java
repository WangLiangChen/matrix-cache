package wang.liangchen.matrix.cache.sdk.override;


import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import wang.liangchen.matrix.cache.sdk.annotation.OverrideBean;
import wang.liangchen.matrix.cache.sdk.cache.mlc.MultilevelMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.serializer.ProtostuffRedisSerializer;
import wang.liangchen.matrix.cache.sdk.consistency.CacheSynchronizer;

import java.util.concurrent.Executors;

/**
 * @author LiangChen.Wang 2020/9/23
 */
// @ConditionalOnProperty("spring.redis.host")
@ConditionalOnClass({Caffeine.class, RedisTemplate.class})
@EnableConfigurationProperties(CacheProperties.class)
class MatrixMultilevelCachingConfiguration {
    @OverrideBean("cacheManager")
    @Bean
    public CacheManager multilevelCacheManager(CacheProperties cacheProperties, ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoader, RedisTemplate<Object, Object> redisTemplate) {
        CacheManager localCacheManager = new MatrixLocalCachingConfiguration().matrixLocalCacheManager(cacheProperties, caffeineCacheLoader);
        CacheManager distributedCacheManager = new MatrixDistributedCachingConfiguration().matrixDistributedCacheManager(cacheProperties, redisTemplate);
        return new MultilevelMatrixCacheManager(localCacheManager, distributedCacheManager, redisTemplate);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, MultilevelMatrixCacheManager multilevelCacheManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.setTaskExecutor(Executors.newFixedThreadPool(16, new CustomizableThreadFactory("mx-listener-")));
        MessageListenerAdapter listener = new MessageListenerAdapter(multilevelCacheManager);
        listener.afterPropertiesSet();
        container.addMessageListener(listener, new ChannelTopic(CacheSynchronizer.EVICT_MESSAGE_TOPIC));
        return container;
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
