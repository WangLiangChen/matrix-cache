package wang.liangchen.matrix.cache.sdk.override;


import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MatrixCacheInterceptor extends org.springframework.cache.interceptor.CacheInterceptor {
    private boolean initialized = false;
    private static final Object NO_RESULT = new Object();
    private static final Object RESULT_UNAVAILABLE = new Object();
    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();
    private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();
        this.initialized = true;
    }

    @Override
    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
        if (!this.initialized) {
            return invoker.invoke();
        }
        CacheOperationSource cacheOperationSource = getCacheOperationSource();
        if (null == cacheOperationSource) {
            return invoker.invoke();
        }
        Class<?> targetClass = getTargetClass(target);
        // 获取注解的解析结果，将注解解析为CacheOperation
        Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
        if (CollectionUtils.isEmpty(operations)) {
            return invoker.invoke();
        }
        return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
    }

    @Nullable
    private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheOperationContext context = contexts.get(MatrixCacheableOperation.class).iterator().next();
            if (isConditionPassing(context, NO_RESULT)) {
                Object key = generateKey(context, NO_RESULT);
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
        processCacheEvicts(contexts.get(CacheEvictOperation.class), true, NO_RESULT);

        // Check if we have a cached item matching the conditions
        Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(MatrixCacheableOperation.class));

        // Collect puts from any @Cacheable miss, if no cached item is found
        List<CachePutRequest> cachePutRequests = new ArrayList<>();
        if (cacheHit == null) {
            collectPutRequests(contexts.get(MatrixCacheableOperation.class), NO_RESULT, cachePutRequests);
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
        collectPutRequests(contexts.get(MatrixCachePutOperation.class), cacheValue, cachePutRequests);

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
        Collection<CacheOperationContext> cachePutContexts = contexts.get(MatrixCachePutOperation.class);
        Collection<CacheOperationContext> excluded = new ArrayList<>();
        for (CacheOperationContext context : cachePutContexts) {
            try {
                if (!context.isConditionPassing(RESULT_UNAVAILABLE)) {
                    excluded.add(context);
                }
            } catch (VariableNotAvailableException ex) {
                // Ignoring failure due to missing result, consider the cache put has to proceed
            }
        }
        // Check if all puts have been excluded by condition
        return (cachePutContexts.size() != excluded.size());
    }

    private void collectPutRequests(Collection<CacheOperationContext> contexts,
                                    @Nullable Object result, Collection<CachePutRequest> putRequests) {

        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                putRequests.add(new CachePutRequest(context, key));
            }
        }
    }

    @Nullable
    private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
        Object result = NO_RESULT;
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

    private void processCacheEvicts(
            Collection<CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {

        for (CacheOperationContext context : contexts) {
            CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    private void performCacheEvict(
            CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {

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

    private void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, @Nullable Object key) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
                    " for operation " + operation + " on method " + context.metadata.method);
        }
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

    @Nullable
    private Object unwrapReturnValue(@Nullable Object returnValue) {
        return ObjectUtils.unwrapOptional(returnValue);
    }

    private boolean isConditionPassing(CacheOperationContext context, @Nullable Object result) {
        boolean passing = context.isConditionPassing(result);
        if (!passing && logger.isTraceEnabled()) {
            logger.trace("Cache condition failed on method " + context.metadata.method +
                    " for operation " + context.metadata.operation);
        }
        return passing;
    }

    private Object generateKey(CacheOperationContext context, @Nullable Object result) {
        Object key = context.generateKey(result);
        if (key == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
                    "using named params on classes without debug info?) " + context.metadata.operation);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache key '" + key + "' for operation " + context.metadata.operation);
        }
        return key;
    }


    @Override
    protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
        CacheOperationMetadata metadata = this.getCacheOperationMetadata(operation, method, targetClass);
        return new CacheOperationContext(metadata, args, target);
    }

    @Override
    protected CacheOperationMetadata getCacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass) {
        CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
        CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
        if (metadata == null) {
            KeyGenerator operationKeyGenerator;
            if (StringUtils.hasText(operation.getKeyGenerator())) {
                operationKeyGenerator = this.getBean(operation.getKeyGenerator(), KeyGenerator.class);
            } else {
                operationKeyGenerator = this.getKeyGenerator();
            }

            CacheResolver operationCacheResolver;
            if (StringUtils.hasText(operation.getCacheResolver())) {
                operationCacheResolver = this.getBean(operation.getCacheResolver(), CacheResolver.class);
            } else if (StringUtils.hasText(operation.getCacheManager())) {
                CacheManager cacheManager = this.getBean(operation.getCacheManager(), CacheManager.class);
                operationCacheResolver = new MatrixCacheResolver(cacheManager);
            } else {
                operationCacheResolver = this.getCacheResolver();
                Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
            }

            metadata = new CacheOperationMetadata(operation, method, targetClass, operationKeyGenerator, operationCacheResolver);
            this.metadataCache.put(cacheKey, metadata);
        }

        return metadata;
    }

    private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {
        private final CacheOperation cacheOperation;
        private final AnnotatedElementKey methodCacheKey;

        private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
            this.cacheOperation = cacheOperation;
            this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
        }

        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof CacheOperationCacheKey)) {
                return false;
            } else {
                CacheOperationCacheKey otherKey = (CacheOperationCacheKey) other;
                return this.cacheOperation.equals(otherKey.cacheOperation) && this.methodCacheKey.equals(otherKey.methodCacheKey);
            }
        }

        public int hashCode() {
            return this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode();
        }

        public String toString() {
            return this.cacheOperation + " on " + this.methodCacheKey;
        }

        public int compareTo(CacheOperationCacheKey other) {
            int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
            if (result == 0) {
                result = this.methodCacheKey.compareTo(other.methodCacheKey);
            }

            return result;
        }
    }

    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
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

    private class CacheOperationContexts {

        private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;

        private final boolean sync;

        public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
                                      Object[] args, Object target, Class<?> targetClass) {

            this.contexts = new LinkedMultiValueMap<>(operations.size());
            for (CacheOperation op : operations) {
                CacheOperationContext cacheOperationContext = getOperationContext(op, method, args, target, targetClass);
                this.contexts.add(op.getClass(), cacheOperationContext);
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
            List<CacheOperationContext> cacheOperationContexts = this.contexts.get(MatrixCacheableOperation.class);
            if (cacheOperationContexts == null) {  // no @Cacheable operation at all
                return false;
            }
            boolean syncEnabled = false;
            for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
                if (((MatrixCacheableOperation) cacheOperationContext.getOperation()).isSync()) {
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
                MatrixCacheableOperation operation = (MatrixCacheableOperation) cacheOperationContext.getOperation();
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

    private class CacheOperationMetadata extends org.springframework.cache.interceptor.CacheAspectSupport.CacheOperationMetadata {
        private final CacheOperation operation;
        private final Method method;

        public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass, KeyGenerator keyGenerator, CacheResolver cacheResolver) {
            super(operation, method, targetClass, keyGenerator, cacheResolver);
            this.operation = operation;
            this.method = method;
        }
    }

    private class CacheOperationContext extends org.springframework.cache.interceptor.CacheAspectSupport.CacheOperationContext {
        private final CacheOperationMetadata metadata;

        public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
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


    private class CacheExpressionRootObject {

        private final Collection<? extends Cache> caches;

        private final Method method;

        private final Object[] args;

        private final Object target;

        private final Class<?> targetClass;


        public CacheExpressionRootObject(
                Collection<? extends Cache> caches, Method method, Object[] args, Object target, Class<?> targetClass) {

            this.method = method;
            this.target = target;
            this.targetClass = targetClass;
            this.args = args;
            this.caches = caches;
        }


        public Collection<? extends Cache> getCaches() {
            return this.caches;
        }

        public Method getMethod() {
            return this.method;
        }

        public String getMethodName() {
            return this.method.getName();
        }

        public Object[] getArgs() {
            return this.args;
        }

        public Object getTarget() {
            return this.target;
        }

        public Class<?> getTargetClass() {
            return this.targetClass;
        }

    }

    private class CacheEvaluationContext extends MethodBasedEvaluationContext {

        private final Set<String> unavailableVariables = new HashSet<>(1);


        CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
                               ParameterNameDiscoverer parameterNameDiscoverer) {

            super(rootObject, method, arguments, parameterNameDiscoverer);
        }


        /**
         * Add the specified variable name as unavailable for that context.
         * Any expression trying to access this variable should lead to an exception.
         * <p>This permits the validation of expressions that could potentially a
         * variable even when such variable isn't available yet. Any expression
         * trying to use that variable should therefore fail to evaluate.
         */
        public void addUnavailableVariable(String name) {
            this.unavailableVariables.add(name);
        }


        /**
         * Load the param information only when needed.
         */
        @Override
        @Nullable
        public Object lookupVariable(String name) {
            if (this.unavailableVariables.contains(name)) {
                throw new VariableNotAvailableException(name);
            }
            return super.lookupVariable(name);
        }

    }

    private class VariableNotAvailableException extends EvaluationException {

        private final String name;


        public VariableNotAvailableException(String name) {
            super("Variable not available");
            this.name = name;
        }


        public final String getName() {
            return this.name;
        }

    }

    private class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

        /**
         * Indicate that there is no result variable.
         */
        public final Object NO_RESULT = new Object();

        /**
         * Indicate that the result variable cannot be used at all.
         */
        public final Object RESULT_UNAVAILABLE = new Object();

        /**
         * The name of the variable holding the result object.
         */
        public static final String RESULT_VARIABLE = "result";


        private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

        private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

        private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<>(64);


        /**
         * Create an {@link EvaluationContext}.
         *
         * @param caches      the current caches
         * @param method      the method
         * @param args        the method arguments
         * @param target      the target object
         * @param targetClass the target class
         * @param result      the return value (can be {@code null}) or
         *                    {@link #NO_RESULT} if there is no return at this time
         * @return the evaluation context
         */
        public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
                                                         Method method, Object[] args, Object target, Class<?> targetClass, Method targetMethod,
                                                         @Nullable Object result, @Nullable BeanFactory beanFactory) {

            CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
                    caches, method, args, target, targetClass);
            CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
                    rootObject, targetMethod, args, getParameterNameDiscoverer());
            if (result == RESULT_UNAVAILABLE) {
                evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
            } else if (result != NO_RESULT) {
                evaluationContext.setVariable(RESULT_VARIABLE, result);
            }
            if (beanFactory != null) {
                evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
            }
            return evaluationContext;
        }

        @Nullable
        public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
        }

        public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
                    evalContext, Boolean.class)));
        }

        public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return (Boolean.TRUE.equals(getExpression(this.unlessCache, methodKey, unlessExpression).getValue(
                    evalContext, Boolean.class)));
        }

        /**
         * Clear all caches.
         */
        void clear() {
            this.keyCache.clear();
            this.conditionCache.clear();
            this.unlessCache.clear();
        }

    }

    private static class InvocationAwareResult {

        boolean invoked;

    }
}
