package wang.liangchen.matrix.easycache.sdk.override;


import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.*;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.lang.reflect.Method;
import java.util.*;

public class CacheInterceptor extends org.springframework.cache.interceptor.CacheInterceptor {

    private boolean initialized = false;

    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();
        this.initialized = true;
    }

    @Override
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

    private Object execute(CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
            if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
                Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
                Cache cache = context.getCaches().iterator().next();
                try {
                    return wrapCacheValue(method, handleSynchronizedGet(invoker, key, cache));
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

    private boolean hasCachePut(CacheOperationContexts contexts) {
        // Evaluate the conditions *without* the result object because we don't have it yet...
        Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
        Collection<CacheAspectSupport.CacheOperationContext> excluded = new ArrayList<>();
        for (CacheOperationContext context : cachePutContexts) {
            try {
                if (!context.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
                    excluded.add(context);
                }
            }
            catch (VariableNotAvailableException ex) {
                // Ignoring failure due to missing result, consider the cache put has to proceed
            }
        }
        // Check if all puts have been excluded by condition
        return (cachePutContexts.size() != excluded.size());
    }
    private void collectPutRequests(Collection<CacheOperationContext> contexts,@Nullable Object result, Collection<CachePutRequest> putRequests) {
        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                putRequests.add(new CachePutRequest(context, key));
            }
        }
    }

    private void processCacheEvicts(Collection<CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {
        for (CacheOperationContext context : contexts) {
            CacheEvictOperation operation = (CacheEvictOperation) context.getOperation();
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    private void performCacheEvict(CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {
        Object key = null;
        for (Cache cache : context.getCaches()) {
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
        if (logger.isTraceEnabled()) {
            logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
                    " for operation " + operation + " on method " + context.getMethod());
        }
    }

    @Nullable
    private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
        Object result = CacheOperationExpressionEvaluator.NO_RESULT;
        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                Cache.ValueWrapper cached = findInCaches(context, key);
                if (cached != null) {
                    return cached;
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
        for (Cache cache : context.getCaches()) {
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

    @Nullable
    private Object handleSynchronizedGet(CacheOperationInvoker invoker, Object key, Cache cache) {
        InvocationAwareResult invocationResult = new InvocationAwareResult();
        Object result = cache.get(key, () -> {
            invocationResult.invoked = true;
            if (logger.isTraceEnabled()) {
                logger.trace("No cache entry for key '" + key + "' in cache " + cache.getName());
            }
            return unwrapReturnValue(invokeOperation(invoker));
        });
        if (!invocationResult.invoked && logger.isTraceEnabled()) {
            logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
        }
        return result;
    }

    private Object generateKey(CacheOperationContext context, @Nullable Object result) {
        Object key = context.generateKey(result);
        if (key == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
                    "using named params on classes without debug info?) " + context.getOperation());
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache key '" + key + "' for operation " + context.getOperation());
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
    private Object unwrapReturnValue(@Nullable Object returnValue) {
        return ObjectUtils.unwrapOptional(returnValue);
    }

    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
    }

    private boolean isConditionPassing(CacheOperationContext context, @Nullable Object result) {
        boolean passing = context.isConditionPassing(result);
        if (!passing && logger.isTraceEnabled()) {
            logger.trace("Cache condition failed on method " + context.getMethod() +
                    " for operation " + context.getOperation());
        }
        return passing;
    }

    private static class InvocationAwareResult {
        boolean invoked;
    }

    private class CachePutRequest {

        private final CacheOperationContext context;

        private final Object key;

        public CachePutRequest(CacheOperationContext context, Object key) {
            this.context = context;
            this.key = key;
        }

        public void apply(@Nullable Object result) {
            if (this.context.canPutToCache(result)) {
                for (Cache cache : this.context.getCaches()) {
                    doPut(cache, this.key, result);
                }
            }
        }
    }

    private class CacheOperationContext extends CacheAspectSupport.CacheOperationContext {
        public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            super(metadata, args, target);
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

    @Override
    protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {

        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new CacheOperationContext(metadata, args, target);
    }

    private class CacheOperationContexts {
        private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;
        private final boolean sync;

        public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
                                      Object[] args, Object target, Class<?> targetClass) {
            this.contexts = new LinkedMultiValueMap<>(operations.size());
            for (CacheOperation op : operations) {
                this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
            }
            this.sync = determineSyncFlag(method);
        }

        public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
            Collection<CacheOperationContext> result = this.contexts.get(operationClass);
            return (result != null ? result : Collections.emptyList());
        }

        public boolean isSynchronized() {
            return this.sync;
        }

        private boolean determineSyncFlag(Method method) {
            List<CacheOperationContext> cacheOperationContexts = this.contexts.get(CacheableOperation.class);
            if (cacheOperationContexts == null) {  // no @Cacheable operation at all
                return false;
            }
            boolean syncEnabled = false;
            for (CacheAspectSupport.CacheOperationContext cacheOperationContext : cacheOperationContexts) {
                if (((org.springframework.cache.interceptor.CacheableOperation) cacheOperationContext.getOperation()).isSync()) {
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
                CacheOperationContext cacheOperationContext = cacheOperationContexts.iterator().next();
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


}
