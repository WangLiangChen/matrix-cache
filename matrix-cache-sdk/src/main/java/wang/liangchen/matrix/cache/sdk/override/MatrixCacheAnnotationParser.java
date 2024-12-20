package wang.liangchen.matrix.cache.sdk.override;

import org.springframework.cache.annotation.*;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import wang.liangchen.matrix.cache.sdk.annotation.CacheExpire;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

/**
 * @author LiangChen.Wang 2021/3/18
 */
public class MatrixCacheAnnotationParser implements CacheAnnotationParser {

    private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<Class<? extends Annotation>>() {{
        add(CacheExpire.class);
        add(Cacheable.class);
        add(CacheEvict.class);
        add(CachePut.class);
        add(Caching.class);
    }};


    @Override
    public boolean isCandidateClass(@NonNull Class<?> targetClass) {
        return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
    }

    @Override
    @Nullable
    public Collection<CacheOperation> parseCacheAnnotations(@NonNull Class<?> type) {
        DefaultCacheConfig defaultConfig = new DefaultCacheConfig(type);
        return parseCacheAnnotations(defaultConfig, type);
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
        DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
        return parseCacheAnnotations(defaultConfig, method);
    }

    private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement annotatedElement) {
        Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, annotatedElement, false);
        if (ops.size() > 1) {
            // More than one operation found -> local declarations override interface-declared ones...
            return parseCacheAnnotations(cachingConfig, annotatedElement, true);
        }
        return ops;
    }


    private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement annotatedElement, boolean localOnly) {
        Collection<? extends Annotation> annotations = (localOnly ?
                AnnotatedElementUtils.getAllMergedAnnotations(annotatedElement, CACHE_OPERATION_ANNOTATIONS) :
                AnnotatedElementUtils.findAllMergedAnnotations(annotatedElement, CACHE_OPERATION_ANNOTATIONS));
        if (annotations.isEmpty()) {
            return Collections.emptyList();
        }
        final Collection<CacheOperation> operations = new ArrayList<>();
        // Find CacheExpire Annotation first
        Duration ttl = Duration.ZERO;
        for (Annotation annotation : annotations) {
            if (annotation instanceof CacheExpire) {
                CacheExpire cacheExpire = (CacheExpire) annotation;
                ttl = Duration.ofMillis(cacheExpire.timeUnit().toMillis(cacheExpire.ttl()));
                break;
            }
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof Cacheable) {
                operations.add(parseCacheableAnnotation(annotatedElement, cachingConfig, (Cacheable) annotation, ttl));
                continue;
            }
            if (annotation instanceof CachePut) {
                operations.add(parsePutAnnotation(annotatedElement, cachingConfig, (CachePut) annotation, ttl));
                continue;
            }
            if (annotation instanceof Caching) {
                parseCachingAnnotation(annotatedElement, cachingConfig, (Caching) annotation, operations, ttl);
                continue;
            }
            if (annotation instanceof CacheEvict) {
                operations.add(parseEvictAnnotation(annotatedElement, cachingConfig, (CacheEvict) annotation));
            }
        }
        return operations;
    }

    private MatrixCacheableOperation parseCacheableAnnotation(AnnotatedElement annotatedElement, DefaultCacheConfig defaultConfig, Cacheable cacheable, Duration ttl) {
        MatrixCacheableOperation.Builder builder = new MatrixCacheableOperation.Builder();
        builder.setName(annotatedElement.toString());
        builder.setCacheNames(cacheable.cacheNames());
        builder.setCondition(cacheable.condition());
        builder.setUnless(cacheable.unless());
        builder.setKey(cacheable.key());
        builder.setKeyGenerator(cacheable.keyGenerator());
        builder.setCacheManager(cacheable.cacheManager());
        builder.setCacheResolver(cacheable.cacheResolver());
        builder.setSync(cacheable.sync());

        builder.setTtl(ttl);

        defaultConfig.applyDefault(builder);
        MatrixCacheableOperation operation = builder.build();
        validateCacheOperation(annotatedElement, operation);
        return operation;
    }

    private CacheEvictOperation parseEvictAnnotation(AnnotatedElement annotatedElement, DefaultCacheConfig defaultConfig, CacheEvict cacheEvict) {
        CacheEvictOperation.Builder builder = new CacheEvictOperation.Builder();
        builder.setName(annotatedElement.toString());
        builder.setCacheNames(cacheEvict.cacheNames());
        builder.setCondition(cacheEvict.condition());
        builder.setKey(cacheEvict.key());
        builder.setKeyGenerator(cacheEvict.keyGenerator());
        builder.setCacheManager(cacheEvict.cacheManager());
        builder.setCacheResolver(cacheEvict.cacheResolver());
        builder.setCacheWide(cacheEvict.allEntries());
        builder.setBeforeInvocation(cacheEvict.beforeInvocation());

        defaultConfig.applyDefault(builder);
        CacheEvictOperation operation = builder.build();
        validateCacheOperation(annotatedElement, operation);
        return operation;
    }

    private CacheOperation parsePutAnnotation(AnnotatedElement annotatedElement, DefaultCacheConfig defaultConfig, CachePut cachePut, Duration ttl) {
        MatrixCachePutOperation.Builder builder = new MatrixCachePutOperation.Builder();
        builder.setName(annotatedElement.toString());
        builder.setCacheNames(cachePut.cacheNames());
        builder.setCondition(cachePut.condition());
        builder.setUnless(cachePut.unless());
        builder.setKey(cachePut.key());
        builder.setKeyGenerator(cachePut.keyGenerator());
        builder.setCacheManager(cachePut.cacheManager());
        builder.setCacheResolver(cachePut.cacheResolver());

        builder.setTtl(ttl);

        defaultConfig.applyDefault(builder);
        MatrixCachePutOperation operation = builder.build();
        validateCacheOperation(annotatedElement, operation);
        return operation;
    }

    private void parseCachingAnnotation(AnnotatedElement annotatedElement, DefaultCacheConfig defaultConfig, Caching caching, Collection<CacheOperation> operations, Duration ttl) {
        Cacheable[] cacheables = caching.cacheable();
        for (Cacheable cacheable : cacheables) {
            operations.add(parseCacheableAnnotation(annotatedElement, defaultConfig, cacheable, ttl));
        }
        CacheEvict[] cacheEvicts = caching.evict();
        for (CacheEvict cacheEvict : cacheEvicts) {
            operations.add(parseEvictAnnotation(annotatedElement, defaultConfig, cacheEvict));
        }
        CachePut[] cachePuts = caching.put();
        for (CachePut cachePut : cachePuts) {
            operations.add(parsePutAnnotation(annotatedElement, defaultConfig, cachePut, ttl));
        }
    }

    private void validateCacheOperation(AnnotatedElement annotatedElement, CacheOperation operation) {
        if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" +
                    annotatedElement.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
                    "These attributes are mutually exclusive: either set the SpEL expression used to" +
                    "compute the key at runtime or set the name of the KeyGenerator bean to use.");
        }
        if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" +
                    annotatedElement.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
                    "These attributes are mutually exclusive: the cache manager is used to configure a" +
                    "default cache resolver if none is set. If a cache resolver is set, the cache manager" +
                    "won't be used.");
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return (other instanceof MatrixCacheAnnotationParser);
    }

    @Override
    public int hashCode() {
        return MatrixCacheAnnotationParser.class.hashCode();
    }


    /**
     * Provides default settings for a given set of cache operations.
     */
    private static class DefaultCacheConfig {

        private final Class<?> target;

        @Nullable
        private String[] cacheNames;

        @Nullable
        private String keyGenerator;

        @Nullable
        private String cacheManager;

        @Nullable
        private String cacheResolver;

        private boolean initialized = false;

        public DefaultCacheConfig(Class<?> target) {
            this.target = target;
        }

        /**
         * Apply the defaults to the specified {@link CacheOperation.Builder}.
         *
         * @param builder the operation builder to update
         */
        public void applyDefault(CacheOperation.Builder builder) {
            if (!this.initialized) {
                CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(this.target, CacheConfig.class);
                if (annotation != null) {
                    this.cacheNames = annotation.cacheNames();
                    this.keyGenerator = annotation.keyGenerator();
                    this.cacheManager = annotation.cacheManager();
                    this.cacheResolver = annotation.cacheResolver();
                }
                this.initialized = true;
            }

            if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
                builder.setCacheNames(this.cacheNames);
            }
            if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
                    StringUtils.hasText(this.keyGenerator)) {
                builder.setKeyGenerator(this.keyGenerator);
            }

            if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
                // One of these is set so we should not inherit anything
            } else if (StringUtils.hasText(this.cacheResolver)) {
                builder.setCacheResolver(this.cacheResolver);
            } else if (StringUtils.hasText(this.cacheManager)) {
                builder.setCacheManager(this.cacheManager);
            }
        }
    }

}
