package wang.liangchen.matrix.mcache.sdk.configuration;


import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.mcache.sdk.override.RedisCacheManager;
import wang.liangchen.matrix.mcache.sdk.redis.RedisCacheCreator;

/**
 * @author LiangChen.Wang 2021/4/14
 */

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnBean(value = {CacheAspectSupport.class, RedisAutoConfiguration.class})
@AutoConfigureAfter(CacheAutoConfiguration.class)
public class RedisCacheAutoConfiguration {

    @Bean
    @ConditionalOnBean(org.springframework.data.redis.cache.RedisCacheManager.class)
    public RedisCacheManager cacheManagerOverride(CacheManagerCustomizers customizers, RedisTemplate<Object, Object> redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate, RedisCacheCreator.INSTANCE.cacheWriter(redisTemplate), RedisCacheConfiguration.defaultCacheConfig());
        return customizers.customize(cacheManager);
    }
}
