/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.SupplierUtils;

/**
 * Base class for caching aspects, such as the {@link CacheInterceptor} or an
 * AspectJ aspect.
 *
 * <p>This enables the underlying Spring caching infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling relevant methods in the correct order.
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@link CacheOperationSource} is
 * used for determining caching operations, a {@link KeyGenerator} will build the
 * cache keys, and a {@link CacheResolver} will resolve the actual cache(s) to use.
 *
 * <p>Note: A cache aspect is serializable but does not perform any actual caching
 * after deserialization.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

	private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

	@Nullable
	private CacheOperationSource cacheOperationSource;

	private SingletonSupplier<KeyGenerator> keyGenerator = SingletonSupplier.of(SimpleKeyGenerator::new);

	@Nullable
	private SingletonSupplier<CacheResolver> cacheResolver;

	@Nullable
	private BeanFactory beanFactory;

	private boolean initialized = false;


	/**
	 * Configure this aspect with the given error handler, key generator and cache resolver/manager
	 * suppliers, applying the corresponding default if a supplier is not resolvable.
	 * @since 5.1
	 */
	public void configure(
			@Nullable Supplier<CacheErrorHandler> errorHandler, @Nullable Supplier<KeyGenerator> keyGenerator,
			@Nullable Supplier<CacheResolver> cacheResolver, @Nullable Supplier<CacheManager> cacheManager) {

		this.errorHandler = new SingletonSupplier<>(errorHandler, SimpleCacheErrorHandler::new);
		this.keyGenerator = new SingletonSupplier<>(keyGenerator, SimpleKeyGenerator::new);
		this.cacheResolver = new SingletonSupplier<>(cacheResolver,
				() -> SimpleCacheResolver.of(SupplierUtils.resolve(cacheManager)));
	}


	/**
	 * Set one or more cache operation sources which are used to find the cache
	 * attributes. If more than one source is provided, they will be aggregated
	 * using a {@link CompositeCacheOperationSource}.
	 * @see #setCacheOperationSource
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "At least 1 CacheOperationSource needs to be specified");
		this.cacheOperationSource = (cacheOperationSources.length > 1 ?
				new CompositeCacheOperationSource(cacheOperationSources) : cacheOperationSources[0]);
	}

	/**
	 * Set the CacheOperationSource for this cache aspect.
	 * @since 5.1
	 * @see #setCacheOperationSources
	 */
	public void setCacheOperationSource(@Nullable CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * Return the CacheOperationSource for this cache aspect.
	 */
	@Nullable
	public CacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	/**
	 * Set the default {@link KeyGenerator} that this cache aspect should delegate to
	 * if no specific key generator has been set for the operation.
	 * <p>The default is a {@link SimpleKeyGenerator}.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = SingletonSupplier.of(keyGenerator);
	}

	/**
	 * Return the default {@link KeyGenerator} that this cache aspect delegates to.
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator.obtain();
	}

	/**
	 * Set the default {@link CacheResolver} that this cache aspect should delegate
	 * to if no specific cache resolver has been set for the operation.
	 * <p>The default resolver resolves the caches against their names and the
	 * default cache manager.
	 * @see #setCacheManager
	 * @see SimpleCacheResolver
	 */
	public void setCacheResolver(@Nullable CacheResolver cacheResolver) {
		this.cacheResolver = SingletonSupplier.ofNullable(cacheResolver);
	}

	/**
	 * Return the default {@link CacheResolver} that this cache aspect delegates to.
	 */
	@Nullable
	public CacheResolver getCacheResolver() {
		return SupplierUtils.resolve(this.cacheResolver);
	}

	/**
	 * Set the {@link CacheManager} to use to create a default {@link CacheResolver}.
	 * Replace the current {@link CacheResolver}, if any.
	 * @see #setCacheResolver
	 * @see SimpleCacheResolver
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheResolver = SingletonSupplier.of(new SimpleCacheResolver(cacheManager));
	}

	/**
	 * Set the containing {@link BeanFactory} for {@link CacheManager} and other
	 * service lookups.
	 * @since 4.3
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSources' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (getCacheResolver() == null) {
			// Lazily initialize cache resolver via default cache manager...
			Assert.state(this.beanFactory != null, "CacheResolver or BeanFactory must be set on cache aspect");
			try {
				setCacheManager(this.beanFactory.getBean(CacheManager.class));
			}
			catch (NoUniqueBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
						"CacheManager found. Mark one as primary or declare a specific CacheManager to use.");
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
						"Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
			}
		}
		this.initialized = true;
	}


	/**
	 * Convenience method to return a String representation of this Method
	 * for use in logging. Can be overridden in subclasses to provide a
	 * different identifier for the given method.
	 * @param method the method we're interested in
	 * @param targetClass class the method is on
	 * @return log message identifying this method
	 * @see org.springframework.util.ClassUtils#getQualifiedMethodName
	 */
	protected String methodIdentification(Method method, Class<?> targetClass) {
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		return ClassUtils.getQualifiedMethodName(specificMethod);
	}

	protected Collection<? extends Cache> getCaches(
			CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

		Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
		if (caches.isEmpty()) {
			throw new IllegalStateException("No cache could be resolved for '" +
					context.getOperation() + "' using resolver '" + cacheResolver +
					"'. At least one cache should be provided per cache operation.");
		}
		return caches;
	}

	protected CacheOperationContext getOperationContext(
			CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {

		CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
		return new CacheOperationContext(metadata, args, target);
	}

	/**
	 * Return the {@link CacheOperationMetadata} for the specified operation.
	 * <p>Resolve the {@link CacheResolver} and the {@link KeyGenerator} to be
	 * used for the operation.
	 * @param operation the operation
	 * @param method the method on which the operation is invoked
	 * @param targetClass the target type
	 * @return the resolved metadata for the operation
	 */
	protected CacheOperationMetadata getCacheOperationMetadata(
			CacheOperation operation, Method method, Class<?> targetClass) {

		CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
		CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
		if (metadata == null) {
			KeyGenerator operationKeyGenerator;
			if (StringUtils.hasText(operation.getKeyGenerator())) {
				operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
			}
			else {
				operationKeyGenerator = getKeyGenerator();
			}
			CacheResolver operationCacheResolver;
			if (StringUtils.hasText(operation.getCacheResolver())) {
				operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
			}
			else if (StringUtils.hasText(operation.getCacheManager())) {
				CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
				operationCacheResolver = new SimpleCacheResolver(cacheManager);
			}
			else {
				operationCacheResolver = getCacheResolver();
				Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
			}
			metadata = new CacheOperationMetadata(operation, method, targetClass,
					operationKeyGenerator, operationCacheResolver);
			this.metadataCache.put(cacheKey, metadata);
		}
		return metadata;
	}

	/**
	 * Return a bean with the specified name and type. Used to resolve services that
	 * are referenced by name in a {@link CacheOperation}.
	 * @param beanName the name of the bean, as defined by the operation
	 * @param expectedType type for the bean
	 * @return the bean matching that name
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if such bean does not exist
	 * @see CacheOperation#getKeyGenerator()
	 * @see CacheOperation#getCacheManager()
	 * @see CacheOperation#getCacheResolver()
	 */
	protected <T> T getBean(String beanName, Class<T> expectedType) {
		if (this.beanFactory == null) {
			throw new IllegalStateException(
					"BeanFactory must be set on cache aspect for " + expectedType.getSimpleName() + " retrieval");
		}
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
	}

	/**
	 * Clear the cached metadata.
	 */
	protected void clearMetadataCache() {
		this.metadataCache.clear();
		this.evaluator.clear();
	}

	@Nullable
	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
		if (this.initialized) {
			Class<?> targetClass = getTargetClass(target);
			CacheOperationSource cacheOperationSource = getCacheOperationSource();
			if (cacheOperationSource != null) {
				Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
				if (!CollectionUtils.isEmpty(operations)) {
					return execute(invoker, method,
							new CacheOperationContexts(operations, method, args, target, targetClass));
				}
			}
		}

		return invoker.invoke();
	}

	/**
	 * Execute the underlying operation (typically in case of cache miss) and return
	 * the result of the invocation. If an exception occurs it will be wrapped in a
	 * {@link CacheOperationInvoker.ThrowableWrapper}: the exception can be handled
	 * or modified but it <em>must</em> be wrapped in a
	 * {@link CacheOperationInvoker.ThrowableWrapper} as well.
	 * @param invoker the invoker handling the operation being cached
	 * @return the result of the invocation
	 * @see CacheOperationInvoker#invoke()
	 */
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}

	private Class<?> getTargetClass(Object target) {
		return AopProxyUtils.ultimateTargetClass(target);
	}

	@Nullable
	private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
		// Special handling of synchronized invocation
		if (contexts.isSynchronized()) {
			CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
			if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
				Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
				Cache cache = context.getCaches().iterator().next();
				try {
					return wrapCacheValue(method, cache.get(key, () -> unwrapReturnValue(invokeOperation(invoker))));
				}
				catch (Cache.ValueRetrievalException ex) {
					// The invoker wraps any Throwable in a ThrowableWrapper instance so we
					// can just make sure that one bubbles up the stack.
					throw (CacheOperationInvoker.ThrowableWrapper) ex.getCause();
				}
			}
			else {
				// No caching required, only call the underlying method
				return invokeOperation(invoker);
			}
		}


		// Process any early evictions
		processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
				CacheOperationExpressionEvaluator.NO_RESULT);

		// Check if we have a cached item matching the conditions
		Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

		// Collect puts from any @Cacheable miss, if no cached item is found
		List<CachePutRequest> cachePutRequests = new LinkedList<>();
		if (cacheHit == null) {
			collectPutRequests(contexts.get(CacheableOperation.class),
					CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
		}

		Object cacheValue;
		Object returnValue;

		if (cacheHit != null && !hasCachePut(contexts)) {
			// If there are no put requests, just use the cache hit
			cacheValue = cacheHit.get();
			returnValue = wrapCacheValue(method, cacheValue);
		}
		else {
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

	@Nullable
	private Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
		if (method.getReturnType() == Optional.class &&
				(cacheValue == null || cacheValue.getClass() != Optional.class)) {
			return Optional.ofNullable(cacheValue);
		}
		return cacheValue;
	}

	@Nullable
	private Object unwrapReturnValue(Object returnValue) {
		return ObjectUtils.unwrapOptional(returnValue);
	}

	private boolean hasCachePut(CacheOperationContexts contexts) {
		// Evaluate the conditions *without* the result object because we don't have it yet...
		Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
		Collection<CacheOperationContext> excluded = new ArrayList<>();
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
				doClear(cache);
			}
			else {
				if (key == null) {
					key = generateKey(context, result);
				}
				logInvalidating(context, operation, key);
				doEvict(cache, key);
			}
		}
	}

	private void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, @Nullable Object key) {
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
					" for operation " + operation + " on method " + context.metadata.method);
		}
	}

	/**
	 * Find a cached item only for {@link CacheableOperation} that passes the condition.
	 * @param contexts the cacheable operations
	 * @return a {@link Cache.ValueWrapper} holding the cached item,
	 * or {@code null} if none is found
	 */
	@Nullable
	private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
		Object result = CacheOperationExpressionEvaluator.NO_RESULT;
		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				Object key = generateKey(context, result);
				Cache.ValueWrapper cached = findInCaches(context, key);
				if (cached != null) {
					return cached;
				}
				else {
					if (logger.isTraceEnabled()) {
						logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
					}
				}
			}
		}
		return null;
	}

	/**
	 * Collect the {@link CachePutRequest} for all {@link CacheOperation} using
	 * the specified result item.
	 * @param contexts the contexts to handle
	 * @param result the result item (never {@code null})
	 * @param putRequests the collection to update
	 */
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
			for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
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


	/**
	 * Metadata of a cache operation that does not depend on a particular invocation
	 * which makes it a good candidate for caching.
	 */
	protected static class CacheOperationMetadata {

		private final CacheOperation operation;

		private final Method method;

		private final Class<?> targetClass;

		private final Method targetMethod;

		private final AnnotatedElementKey methodKey;

		private final KeyGenerator keyGenerator;

		private final CacheResolver cacheResolver;

		public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass,
				KeyGenerator keyGenerator, CacheResolver cacheResolver) {

			this.operation = operation;
			this.method = BridgeMethodResolver.findBridgedMethod(method);
			this.targetClass = targetClass;
			this.targetMethod = (!Proxy.isProxyClass(targetClass) ?
					AopUtils.getMostSpecificMethod(method, targetClass) : this.method);
			this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);
			this.keyGenerator = keyGenerator;
			this.cacheResolver = cacheResolver;
		}
	}


	/**
	 * A {@link CacheOperationInvocationContext} context for a {@link CacheOperation}.
	 */
	protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

		private final CacheOperationMetadata metadata;

		private final Object[] args;

		private final Object target;

		private final Collection<? extends Cache> caches;

		private final Collection<String> cacheNames;

		@Nullable
		private Boolean conditionPassing;

		public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
			this.metadata = metadata;
			this.args = extractArgs(metadata.method, args);
			this.target = target;
			this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
			this.cacheNames = createCacheNames(this.caches);
		}

		@Override
		public CacheOperation getOperation() {
			return this.metadata.operation;
		}

		@Override
		public Object getTarget() {
			return this.target;
		}

		@Override
		public Method getMethod() {
			return this.metadata.method;
		}

		@Override
		public Object[] getArgs() {
			return this.args;
		}

		private Object[] extractArgs(Method method, Object[] args) {
			if (!method.isVarArgs()) {
				return args;
			}
			Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
			Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
			System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
			System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
			return combinedArgs;
		}

		protected boolean isConditionPassing(@Nullable Object result) {
			if (this.conditionPassing == null) {
				if (StringUtils.hasText(this.metadata.operation.getCondition())) {
					EvaluationContext evaluationContext = createEvaluationContext(result);
					this.conditionPassing = evaluator.condition(this.metadata.operation.getCondition(),
							this.metadata.methodKey, evaluationContext);
				}
				else {
					this.conditionPassing = true;
				}
			}
			return this.conditionPassing;
		}

		protected boolean canPutToCache(@Nullable Object value) {
			String unless = "";
			if (this.metadata.operation instanceof CacheableOperation) {
				unless = ((CacheableOperation) this.metadata.operation).getUnless();
			}
			else if (this.metadata.operation instanceof CachePutOperation) {
				unless = ((CachePutOperation) this.metadata.operation).getUnless();
			}
			if (StringUtils.hasText(unless)) {
				EvaluationContext evaluationContext = createEvaluationContext(value);
				return !evaluator.unless(unless, this.metadata.methodKey, evaluationContext);
			}
			return true;
		}

		/**
		 * Compute the key for the given caching operation.
		 */
		@Nullable
		protected Object generateKey(@Nullable Object result) {
			if (StringUtils.hasText(this.metadata.operation.getKey())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.key(this.metadata.operation.getKey(), this.metadata.methodKey, evaluationContext);
			}
			return this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args);
		}

		private EvaluationContext createEvaluationContext(@Nullable Object result) {
			return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
					this.target, this.metadata.targetClass, this.metadata.targetMethod, result, beanFactory);
		}

		protected Collection<? extends Cache> getCaches() {
			return this.caches;
		}

		protected Collection<String> getCacheNames() {
			return this.cacheNames;
		}

		private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
			Collection<String> names = new ArrayList<>();
			for (Cache cache : caches) {
				names.add(cache.getName());
			}
			return names;
		}
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


	private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

		private final CacheOperation cacheOperation;

		private final AnnotatedElementKey methodCacheKey;

		private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
			this.cacheOperation = cacheOperation;
			this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
		}

		@Override
		public boolean equals(Object other) {
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
