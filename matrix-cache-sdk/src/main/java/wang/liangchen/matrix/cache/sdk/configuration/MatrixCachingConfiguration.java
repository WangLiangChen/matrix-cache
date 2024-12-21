package wang.liangchen.matrix.cache.sdk.configuration;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.util.function.SingletonSupplier;
import wang.liangchen.matrix.cache.sdk.generator.MatrixKeyGenerator;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheAnnotationParser;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheInterceptor;
import wang.liangchen.matrix.cache.sdk.override.MatrixCacheResolver;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author LiangChen.Wang 2020/9/23
 */
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class MatrixCachingConfiguration {
    private static Supplier<CacheManager> cacheManager;
    private static Supplier<CacheResolver> cacheResolver;
    private static Supplier<KeyGenerator> keyGenerator;
    private static Supplier<CacheErrorHandler> errorHandler;

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static CacheOperationSource cacheOperationSourceOverride() {
        AnnotationCacheOperationSource cacheOperationSource = new AnnotationCacheOperationSource(new MatrixCacheAnnotationParser());
        cacheOperationSource.setPublicMethodsOnly(false);
        return cacheOperationSource;
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
                return new MatrixCacheResolver(cacheManager);
            }

            @Override
            public KeyGenerator keyGenerator() {
                return new MatrixKeyGenerator();
            }

            @Override
            public CacheErrorHandler errorHandler() {
                return new SimpleCacheErrorHandler();
            }
        };
    }

    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static MatrixCacheInterceptor cacheInterceptorOverride(CacheOperationSource cacheOperationSource, ObjectProvider<CachingConfigurer> cachingConfigurerProvider) {
        MatrixCacheInterceptor matrixCacheInterceptor = new MatrixCacheInterceptor();
        matrixCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        // apply config by supplier
        CachingConfigurerSupplier cachingConfigurerSupplier = new CachingConfigurerSupplier(cachingConfigurerProvider::getIfAvailable);
        Supplier<CacheManager> cacheManager = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheManager);
        Supplier<CacheResolver> cacheResolver = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheResolver);
        Supplier<KeyGenerator> keyGenerator = cachingConfigurerSupplier.adapt(CachingConfigurer::keyGenerator);
        Supplier<CacheErrorHandler> errorHandler = cachingConfigurerSupplier.adapt(CachingConfigurer::errorHandler);
        matrixCacheInterceptor.configure(errorHandler, keyGenerator, cacheResolver, cacheManager);
        return matrixCacheInterceptor;
    }

    static class CachingConfigurerSupplier {
        private final Supplier<CachingConfigurer> supplier;

        public CachingConfigurerSupplier(Supplier<CachingConfigurer> supplier) {
            this.supplier = SingletonSupplier.of(supplier);
        }

        public <T> Supplier<T> adapt(Function<CachingConfigurer, T> provider) {
            return () -> {
                CachingConfigurer cachingConfigurer = this.supplier.get();
                return (cachingConfigurer != null ? provider.apply(cachingConfigurer) : null);
            };
        }

    }
}
