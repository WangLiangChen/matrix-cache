package wang.liangchen.matrix.mcache.sdk.override;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LiangChen.Wang
 */
public class CacheInterceptor extends org.springframework.cache.interceptor.CacheInterceptor {
    private boolean initialized = false;
    private final Map<CacheOperationCacheKey, CacheAspectSupport.CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

    @Override
    @Nullable
    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
        if (this.initialized) {
            Class<?> targetClass = getTargetClass(target);
            CacheOperationSource cacheOperationSource = getCacheOperationSource();
            if (cacheOperationSource != null) {
                Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
                if (!CollectionUtils.isEmpty(operations)) {
                    return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
                }
            }
        }
        return invoker.invoke();
    }

    @Nullable
    private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheAspectSupport.CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
            if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
                Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
                CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
                Cache cache = cacheOperationContext.getCaches().iterator().next();
                CacheableOperation operation = (CacheableOperation) cacheOperationContext.getOperation();
                try {
                    Object returnValue = handleSynchronizedGet(invoker, key, cache, operation.getTtl());
                    return wrapCacheValue(method, returnValue);
                } catch (Cache.ValueRetrievalException ex) {
                    // Directly propagate ThrowableWrapper from the invoker,
                    // or potentially also an IllegalArgumentException etc.
                    ReflectionUtils.rethrowRuntimeException(ex.getCause());
                }
            } else {
                // No caching required, only call the underlying method
                return invokeOperation(invoker);
            }
        }
        // Process any early evictions
        processCacheEvicts(contexts.get(CacheEvictOperation.class), true, CacheOperationExpressionEvaluator.NO_RESULT);
        // Check if we have a cached item matching the conditions
        Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

        // Collect puts from any @Cacheable miss, if no cached item is found
        List<CachePutRequest> cachePutRequests = new ArrayList<>();
        if (cacheHit == null) {
            collectPutRequests(contexts.get(CacheableOperation.class), CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
        }

        Object cacheValue;
        Object returnValue;

        if (cacheHit != null && !hasCachePut(contexts)) {
            // If there are no put requests, just use the cache hit
            cacheValue = cacheHit.get();
            returnValue = wrapCacheValue(method, cacheValue);
        } else {
            // Invoke the method if we don't have a cache hit
            returnValue = invokeOperation(invoker);
            cacheValue = unwrapReturnValue(returnValue);
        }

        // Collect any explicit @CachePuts
        collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

        // Process any collected put requests, either from @CachePut or a @Cacheable miss
        for (CachePutRequest cachePutRequest : cachePutRequests) {
            cachePutRequest.apply(cacheValue);
        }

        // Process any late evictions
        processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);

        return returnValue;
    }


    protected void doPut(Cache cache, Object key, Object result, long ttl) {
        try {
            cache.put(key, result);
        } catch (RuntimeException ex) {
            getErrorHandler().handleCachePutError(ex, cache, key, result);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();
        this.initialized = true;
    }

    @Override
    protected CacheAspectSupport.CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
        CacheAspectSupport.CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new CacheOperationContext(metadata, args, target);
    }

    @Override
    protected CacheAspectSupport.CacheOperationMetadata getCacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass) {
        CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
        CacheAspectSupport.CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
        if (metadata == null) {
            KeyGenerator operationKeyGenerator;
            if (StringUtils.hasText(operation.getKeyGenerator())) {
                operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
            } else {
                operationKeyGenerator = getKeyGenerator();
            }
            CacheResolver operationCacheResolver;
            if (StringUtils.hasText(operation.getCacheResolver())) {
                operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
            } else if (StringUtils.hasText(operation.getCacheManager())) {
                CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
                operationCacheResolver = new SimpleCacheResolver(cacheManager);
            } else {
                operationCacheResolver = getCacheResolver();
                Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
            }
            metadata = new CacheOperationMetadata(operation, method, targetClass,
                    operationKeyGenerator, operationCacheResolver);
            this.metadataCache.put(cacheKey, metadata);
        }
        return metadata;
    }

    class CacheOperationMetadata extends CacheAspectSupport.CacheOperationMetadata {
        private final CacheOperation operation;
        private final Method method;

        public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass, KeyGenerator keyGenerator, CacheResolver cacheResolver) {
            super(operation, method, targetClass, keyGenerator, cacheResolver);
            this.operation = operation;
            this.method = method;
        }
    }

    class CacheOperationContext extends CacheAspectSupport.CacheOperationContext {
        private final CacheAspectSupport.CacheOperationMetadata metadata;

        public CacheOperationContext(CacheAspectSupport.CacheOperationMetadata metadata, Object[] args, Object target) {
            super(metadata, args, target);
            this.metadata = metadata;

        }

        @Override
        protected Collection<? extends Cache> getCaches() {
            return super.getCaches();
        }

        @Override
        protected boolean isConditionPassing(Object result) {
            return super.isConditionPassing(result);
        }

        @Override
        protected Object generateKey(Object result) {
            return super.generateKey(result);
        }

        @Override
        protected Collection<String> getCacheNames() {
            return super.getCacheNames();
        }

        @Override
        protected boolean canPutToCache(Object value) {
            return super.canPutToCache(value);
        }
    }


    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
    }

    private class CacheOperationContexts {

        private final MultiValueMap<Class<? extends CacheOperation>, CacheAspectSupport.CacheOperationContext> contexts;

        private final boolean sync;

        public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
                                      Object[] args, Object target, Class<?> targetClass) {

            this.contexts = new LinkedMultiValueMap<>(operations.size());
            for (CacheOperation op : operations) {
                this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
            }
            this.sync = determineSyncFlag(method);
        }

        public Collection<CacheAspectSupport.CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
            Collection<CacheAspectSupport.CacheOperationContext> result = this.contexts.get(operationClass);
            return (result != null ? result : Collections.emptyList());
        }

        public boolean isSynchronized() {
            return this.sync;
        }

        private boolean determineSyncFlag(Method method) {
            List<CacheAspectSupport.CacheOperationContext> cacheOperationContexts = this.contexts.get(CacheableOperation.class);
            if (cacheOperationContexts == null) {  // no @Cacheable operation at all
                return false;
            }
            boolean syncEnabled = false;
            for (CacheAspectSupport.CacheOperationContext cacheOperationContext : cacheOperationContexts) {
                if (((CacheableOperation) cacheOperationContext.getOperation()).isSync()) {
                    syncEnabled = true;
                    break;
                }
            }
            if (syncEnabled) {
                if (this.contexts.size() > 1) {
                    throw new IllegalStateException(
                            "@Cacheable(sync=true) cannot be combined with other cache operations on '" + method + "'");
                }
                if (cacheOperationContexts.size() > 1) {
                    throw new IllegalStateException(
                            "Only one @Cacheable(sync=true) entry is allowed on '" + method + "'");
                }
                CacheOperationContext cacheOperationContext = (CacheOperationContext) cacheOperationContexts.iterator().next();
                CacheableOperation operation = (CacheableOperation) cacheOperationContext.getOperation();
                if (cacheOperationContext.getCaches().size() > 1) {
                    throw new IllegalStateException(
                            "@Cacheable(sync=true) only allows a single cache on '" + operation + "'");
                }
                if (StringUtils.hasText(operation.getUnless())) {
                    throw new IllegalStateException(
                            "@Cacheable(sync=true) does not support unless attribute on '" + operation + "'");
                }
                return true;
            }
            return false;
        }
    }

    private boolean isConditionPassing(CacheAspectSupport.CacheOperationContext context, @Nullable Object result) {
        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
        boolean passing = cacheOperationContext.isConditionPassing(result);
        if (!passing && logger.isTraceEnabled()) {
            CacheOperationMetadata metadata = (CacheOperationMetadata) cacheOperationContext.metadata;
            logger.trace("Cache condition failed on method " + metadata.method + " for operation " + metadata.operation);
        }
        return passing;
    }

    private Object generateKey(CacheAspectSupport.CacheOperationContext context, @Nullable Object result) {
        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
        Object key = cacheOperationContext.generateKey(result);
        CacheOperationMetadata metadata = (CacheOperationMetadata) cacheOperationContext.metadata;
        if (key == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
                    "using named params on classes without debug info?) " + metadata.operation);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache key '" + key + "' for operation " + metadata.operation);
        }
        return key;
    }

    @Nullable
    private Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
        if (method.getReturnType() == Optional.class &&
                (cacheValue == null || cacheValue.getClass() != Optional.class)) {
            return Optional.ofNullable(cacheValue);
        }
        return cacheValue;
    }

    @Nullable
    private Object handleSynchronizedGet(CacheOperationInvoker invoker, Object key, Cache cache, long ttl) {
        InvocationAwareResult invocationResult = new InvocationAwareResult();
        Object result = cache.get(key, callable(invocationResult, invoker, key, cache));
        if (!invocationResult.invoked && logger.isTraceEnabled()) {
            logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
        }
        return result;
    }

    private Callable<Object> callable(InvocationAwareResult invocationResult, CacheOperationInvoker invoker, Object key, Cache cache) {
        return () -> {
            invocationResult.invoked = true;
            if (logger.isTraceEnabled()) {
                logger.trace("No cache entry for key '" + key + "' in cache " + cache.getName());
            }
            return unwrapReturnValue(invokeOperation(invoker));
        };
    }

    @Nullable
    private Object unwrapReturnValue(@Nullable Object returnValue) {
        return ObjectUtils.unwrapOptional(returnValue);
    }

    private static class InvocationAwareResult {
        boolean invoked;
    }


    private void processCacheEvicts(Collection<CacheAspectSupport.CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {
        for (CacheAspectSupport.CacheOperationContext context : contexts) {
            CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
            CacheOperationMetadata metadata = (CacheOperationMetadata) cacheOperationContext.metadata;
            CacheEvictOperation operation = (CacheEvictOperation) metadata.operation;
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    private void performCacheEvict(CacheAspectSupport.CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {
        Object key = null;
        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
        for (Cache cache : cacheOperationContext.getCaches()) {
            if (operation.isCacheWide()) {
                logInvalidating(context, operation, null);
                doClear(cache, operation.isBeforeInvocation());
            } else {
                if (key == null) {
                    key = generateKey(context, result);
                }
                logInvalidating(context, operation, key);
                doEvict(cache, key, operation.isBeforeInvocation());
            }
        }
    }

    private void logInvalidating(CacheAspectSupport.CacheOperationContext context, CacheEvictOperation operation, @Nullable Object key) {
        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
        if (logger.isTraceEnabled()) {
            CacheOperationMetadata metadata = (CacheOperationMetadata) cacheOperationContext.metadata;
            logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
                    " for operation " + operation + " on method " + metadata.method);
        }
    }

    @Nullable
    private Cache.ValueWrapper findCachedItem(Collection<CacheAspectSupport.CacheOperationContext> contexts) {
        Object result = CacheOperationExpressionEvaluator.NO_RESULT;
        for (CacheAspectSupport.CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                Cache.ValueWrapper cached = findInCaches(context, key);
                if (cached != null) {
                    return cached;
                } else {
                    if (logger.isTraceEnabled()) {
                        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
                        logger.trace("No cache entry for key '" + key + "' in cache(s) " + cacheOperationContext.getCacheNames());
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private Cache.ValueWrapper findInCaches(CacheAspectSupport.CacheOperationContext context, Object key) {
        CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
        for (Cache cache : cacheOperationContext.getCaches()) {
            Cache.ValueWrapper wrapper = doGet(cache, key);
            if (wrapper != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
                }
                return wrapper;
            }
        }
        return null;
    }

    private void collectPutRequests(Collection<CacheAspectSupport.CacheOperationContext> contexts, @Nullable Object result, Collection<CachePutRequest> putRequests) {
        for (CacheAspectSupport.CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                putRequests.add(new CachePutRequest(context, key));
            }
        }
    }

    private boolean hasCachePut(CacheOperationContexts contexts) {
        // Evaluate the conditions *without* the result object because we don't have it yet...
        Collection<CacheAspectSupport.CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
        Collection<CacheAspectSupport.CacheOperationContext> excluded = new ArrayList<>();
        for (CacheAspectSupport.CacheOperationContext context : cachePutContexts) {
            try {
                CacheOperationContext cacheOperationContext = (CacheOperationContext) context;
                if (!cacheOperationContext.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
                    excluded.add(context);
                }
            } catch (VariableNotAvailableException ex) {
                // Ignoring failure due to missing result, consider the cache put has to proceed
            }
        }
        // Check if all puts have been excluded by condition
        return (cachePutContexts.size() != excluded.size());
    }

    private class CachePutRequest {

        private final CacheAspectSupport.CacheOperationContext context;

        private final Object key;

        public CachePutRequest(CacheAspectSupport.CacheOperationContext context, Object key) {
            this.context = context;
            this.key = key;
        }

        public void apply(@Nullable Object result) {
            CacheOperationContext cacheOperationContext = (CacheOperationContext) this.context;
            CacheOperation operation = cacheOperationContext.getOperation();
            if (cacheOperationContext.canPutToCache(result)) {
                for (Cache cache : cacheOperationContext.getCaches()) {
                    if (operation instanceof CachePutOperation) {
                        doPut(cache, this.key, result, ((CachePutOperation) operation).getTtl());
                        continue;
                    }
                    if (operation instanceof CacheableOperation) {
                        doPut(cache, this.key, result, ((CacheableOperation) operation).getTtl());
                        continue;
                    }
                    doPut(cache, this.key, result);
                }
            }
        }
    }

    private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

        private final CacheOperation cacheOperation;

        private final AnnotatedElementKey methodCacheKey;

        private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
            this.cacheOperation = cacheOperation;
            this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheOperationCacheKey)) {
                return false;
            }
            CacheOperationCacheKey otherKey = (CacheOperationCacheKey) other;
            return (this.cacheOperation.equals(otherKey.cacheOperation) &&
                    this.methodCacheKey.equals(otherKey.methodCacheKey));
        }

        @Override
        public int hashCode() {
            return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
        }

        @Override
        public String toString() {
            return this.cacheOperation + " on " + this.methodCacheKey;
        }

        @Override
        public int compareTo(CacheOperationCacheKey other) {
            int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
            if (result == 0) {
                result = this.methodCacheKey.compareTo(other.methodCacheKey);
            }
            return result;
        }
    }
}
