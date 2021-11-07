package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.aop.support.AopUtils;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LiangChen.Wang 2021/3/25
 */
public class AnnotationCacheOperationSource extends org.springframework.cache.annotation.AnnotationCacheOperationSource {
    private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();
    private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);

    public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
        super(annotationParser);
    }

    @Override
    public Collection<CacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
        Collection<CacheOperation> cacheOperations = super.getCacheOperations(method, targetClass);
        if (CollectionUtils.isEmpty(cacheOperations)) {
            return computeCacheOperations(method, targetClass);
        }
        return cacheOperations;
    }

    private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        if (method == specificMethod) {
            return null;
        }
        Object cacheKey = getCacheKey(method, targetClass);
        Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);
        if (cached != null) {
            return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
        }
        cached = findCacheOperations(targetClass);
        if (cached != null) {
            this.attributeCache.put(cacheKey, cached);
        } else {
            this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
        }
        return cached;
    }
}
