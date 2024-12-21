package wang.liangchen.matrix.cache.sdk.configuration;

import com.github.benmanes.caffeine.cache.CacheLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import wang.liangchen.matrix.cache.sdk.cache.caffeine.MatrixCaffeineMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.mlc.MultiLevelMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.MatrixRedisMatrixCacheManager;
import wang.liangchen.matrix.cache.sdk.cache.redis.RedisMessageListener;
import wang.liangchen.matrix.cache.sdk.consistency.RedisSynchronizer;

import java.util.concurrent.Executors;

public class MatrixMultiLevelCachingConfiguration {
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager(CacheProperties cacheProperties,
                                               ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoaderProvider,
                                               ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        CacheManager localeCacheManager = new MatrixLocalCachingConfiguration().localCacheManager(cacheProperties, caffeineCacheLoaderProvider);
        CacheManager distributedCacheManager = new MatrixDistributedCachingConfiguration().distributedCacheManager(cacheProperties, redisConnectionFactoryProvider);
        MultiLevelMatrixCacheManager cacheManager = new MultiLevelMatrixCacheManager();
        cacheManager.setLocalCacheManager((MatrixCaffeineMatrixCacheManager) localeCacheManager);
        cacheManager.setDistributedCacheManager((MatrixRedisMatrixCacheManager) distributedCacheManager);
        return cacheManager;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider, MultiLevelMatrixCacheManager multilevelCacheManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        //container.setConnectionFactory(redisConnectionFactory);
        redisConnectionFactoryProvider.ifAvailable(container::setConnectionFactory);
        container.setTaskExecutor(Executors.newFixedThreadPool(4, new CustomizableThreadFactory("mx-redis-listener-")));
        MessageListenerAdapter listener = new MessageListenerAdapter(new RedisMessageListener());
        listener.afterPropertiesSet();
        container.addMessageListener(listener, new ChannelTopic(RedisSynchronizer.EVICT_MESSAGE_TOPIC));
        return container;
    }
}
