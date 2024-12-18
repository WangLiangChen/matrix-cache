package wang.liangchen.matrix.cache.sdk.override;


import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import wang.liangchen.matrix.cache.sdk.cache.caffeine.MatrixCaffeineMatrixCacheManager;

/**
 * @author LiangChen.Wang 2020/9/23
 */
@ConditionalOnClass(Caffeine.class)
@ConditionalOnMissingClass("org.springframework.data.redis.core.RedisTemplate")
@EnableConfigurationProperties(CacheProperties.class)
class MatrixLocalCachingConfiguration {

    @Bean
    public CacheManager matrixLocalCacheManager(CacheProperties cacheProperties, ObjectProvider<CacheLoader<Object, Object>> caffeineCacheLoader) {
        MatrixCaffeineMatrixCacheManager cacheManager = new MatrixCaffeineMatrixCacheManager();
        caffeineCacheLoader.ifAvailable(cacheManager::setCacheLoader);
        String specification = cacheProperties.getCaffeine().getSpec();
        if (StringUtils.hasText(specification)) {
            cacheManager.setCacheSpecification(specification);
        }
        return cacheManager;
    }

}
