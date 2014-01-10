/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for caching aspects, such as the {@link CacheInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring caching infrastructure to be
 * used easily to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in
 * the correct order.
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@link CacheManager}
 * implementation will perform the actual cache management, and a
 * {@link CacheOperationSource} is used for determining caching
 * operations.
 *
 * <p>A cache aspect is serializable if its {@code CacheManager} and
 * {@code CacheOperationSource} are serializable.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 */
public abstract class CacheAspectSupport implements InitializingBean, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

	private CacheManager cacheManager;

	private CacheOperationSource cacheOperationSource;

	private KeyGenerator keyGenerator = new SimpleKeyGenerator();

	private ApplicationContext applicationContext;

	private boolean initialized = false;


	/**
	 * Set the CacheManager that this cache aspect should delegate to.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Return the CacheManager that this cache aspect delegates to.
	 */
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * Set one or more cache operation sources which are used to find the cache
	 * attributes. If more than one source is provided, they will be aggregated using a
	 * {@link CompositeCacheOperationSource}.
	 * @param cacheOperationSources must not be {@code null}
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources);
		this.cacheOperationSource = (cacheOperationSources.length > 1 ?
				new CompositeCacheOperationSource(cacheOperationSources) : cacheOperationSources[0]);
	}

	/**
	 * Return the CacheOperationSource for this cache aspect.
	 */
	public CacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	/**
	 * Set the default {@link KeyGenerator} that this cache aspect should delegate to
	 * if no specific key generator has been set for the operation.
	 * <p>The default is a {@link SimpleKeyGenerator}
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Return the default {@link KeyGenerator} that this cache aspect delegates to.
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void afterPropertiesSet() {
		Assert.state(this.cacheManager != null, "'cacheManager' is required");
		Assert.state(this.cacheOperationSource != null, "The 'cacheOperationSources' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
		Assert.state(this.applicationContext != null, "The application context was not injected as it should.");
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

	protected Collection<? extends Cache> getCaches(CacheOperation operation) {
		Set<String> cacheNames = operation.getCacheNames();
		Collection<Cache> caches = new ArrayList<Cache>(cacheNames.size());
		for (String cacheName : cacheNames) {
			Cache cache = this.cacheManager.getCache(cacheName);
			Assert.notNull(cache, "Cannot find cache named '" + cacheName + "' for " + operation);
			caches.add(cache);
		}
		return caches;
	}

	protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args,
			Object target, Class<?> targetClass) {

		return new CacheOperationContext(operation, method, args, target, targetClass);
	}

	protected Object execute(Invoker invoker, Object target, Method method, Object[] args) {
		// check whether aspect is enabled
		// to cope with cases where the AJ is pulled in automatically
		if (this.initialized) {
			Class<?> targetClass = getTargetClass(target);
			Collection<CacheOperation> operations = getCacheOperationSource().getCacheOperations(method, targetClass);
			if (!CollectionUtils.isEmpty(operations)) {
				return execute(invoker, new CacheOperationContexts(operations, method, args, target, targetClass));
			}
		}

		return invoker.invoke();
	}

	private Class<?> getTargetClass(Object target) {
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
		if (targetClass == null && target != null) {
			targetClass = target.getClass();
		}
		return targetClass;
	}

	private Object execute(Invoker invoker, CacheOperationContexts contexts) {
		// Process any early evictions
		processCacheEvicts(contexts.get(CacheEvictOperation.class), true, ExpressionEvaluator.NO_RESULT);

		// Collect puts from any @Cachable miss
		List<CachePutRequest> cachePutRequests = new ArrayList<CachePutRequest>();
		collectPutRequests(contexts.get(CacheableOperation.class), ExpressionEvaluator.NO_RESULT, cachePutRequests, true);

		ValueWrapper result = null;

		// We only attempt to get a cached result if there are no put requests
		if (cachePutRequests.isEmpty() && contexts.get(CachePutOperation.class).isEmpty()) {
			result = findCachedResult(contexts.get(CacheableOperation.class));
		}

		// Invoke the method if don't have a cache hit
		if (result == null) {
			result = new SimpleValueWrapper(invoker.invoke());
		}

		// Collect any explicit @CachePuts
		collectPutRequests(contexts.get(CachePutOperation.class), result.get(), cachePutRequests, false);

		// Process any collected put requests, either from @CachePut or a @Cacheable miss
		for (CachePutRequest cachePutRequest : cachePutRequests) {
			cachePutRequest.apply(result.get());
		}

		// Process any late evictions
		processCacheEvicts(contexts.get(CacheEvictOperation.class), false, result.get());

		return result.get();
	}

	private void processCacheEvicts(Collection<CacheOperationContext> contexts, boolean beforeInvocation, Object result) {
		for (CacheOperationContext context : contexts) {
			CacheEvictOperation operation = (CacheEvictOperation) context.operation;
			if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
				performCacheEvict(context, operation, result);
			}
		}
	}

	private void performCacheEvict(CacheOperationContext context, CacheEvictOperation operation, Object result) {
		Object key = null;
		for (Cache cache : context.getCaches()) {
			if (operation.isCacheWide()) {
				logInvalidating(context, operation, null);
				cache.clear();
			}
			else {
				if (key == null) {
					key = context.generateKey(result);
				}
				logInvalidating(context, operation, key);
				cache.evict(key);
			}
		}
	}

	private void logInvalidating(CacheOperationContext context, CacheEvictOperation operation, Object key) {
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
					" for operation " + operation + " on method " + context.method);
		}
	}

	private void collectPutRequests(Collection<CacheOperationContext> contexts,
			Object result, Collection<CachePutRequest> putRequests, boolean whenNotInCache) {

		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, result)) {
				Object key = generateKey(context, result);
				if (!whenNotInCache || findInAnyCaches(contexts, key) == null) {
					putRequests.add(new CachePutRequest(context, key));
				}
			}
		}
	}

	private Cache.ValueWrapper findCachedResult(Collection<CacheOperationContext> contexts) {
		ValueWrapper result = null;
		for (CacheOperationContext context : contexts) {
			if (isConditionPassing(context, ExpressionEvaluator.NO_RESULT)) {
				if (result == null) {
					result = findInCaches(context, generateKey(context, ExpressionEvaluator.NO_RESULT));
				}
			}
		}
		return result;
	}

	private Cache.ValueWrapper  findInAnyCaches(Collection<CacheOperationContext> contexts, Object key) {
		for (CacheOperationContext context : contexts) {
			ValueWrapper wrapper = findInCaches(context, key);
			if (wrapper != null) {
				return wrapper;
			}
		}
		return null;
	}

	private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
		for (Cache cache : context.getCaches()) {
			Cache.ValueWrapper wrapper = cache.get(key);
			if (wrapper != null) {
				return wrapper;
			}
		}
		return null;
	}

	private boolean isConditionPassing(CacheOperationContext context, Object result) {
		boolean passing = context.isConditionPassing(result);
		if (!passing && logger.isTraceEnabled()) {
			logger.trace("Cache condition failed on method " + context.method + " for operation " + context.operation);
		}
		return passing;
	}

	private Object generateKey(CacheOperationContext context, Object result) {
		Object key = context.generateKey(result);
		Assert.notNull(key, "Null key returned for cache operation (maybe you are using named params " +
				"on classes without debug info?) " + context.operation);
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache key " + key + " for operation " + context.operation);
		}
		return key;
	}


	public interface Invoker {

		Object invoke();
	}


	private class CacheOperationContexts {

		private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts =
				new LinkedMultiValueMap<Class<? extends CacheOperation>, CacheOperationContext>();

		public CacheOperationContexts(Collection<? extends CacheOperation> operations,
				Method method, Object[] args, Object target, Class<?> targetClass) {

			for (CacheOperation operation : operations) {
				this.contexts.add(operation.getClass(), getOperationContext(operation, method, args, target, targetClass));
			}
		}

		public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
			Collection<CacheOperationContext> result = this.contexts.get(operationClass);
			return (result != null ? result : Collections.<CacheOperationContext> emptyList());
		}
	}


	protected class CacheOperationContext {

		private final CacheOperation operation;

		private final Method method;

		private final Object[] args;

		private final Object target;

		private final Class<?> targetClass;

		private final Collection<? extends Cache> caches;

		private final KeyGenerator operationKeyGenerator;

		public CacheOperationContext(CacheOperation operation, Method method,
									 Object[] args, Object target, Class<?> targetClass) {
			this.operation = operation;
			this.method = method;
			this.args = extractArgs(method, args);
			this.target = target;
			this.targetClass = targetClass;
			this.caches = CacheAspectSupport.this.getCaches(operation);
			if (StringUtils.hasText(operation.getKeyGenerator())) { // TODO: exception mgt?
				this.operationKeyGenerator = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
						applicationContext, KeyGenerator.class, operation.getKeyGenerator());
			} else {
				this.operationKeyGenerator = keyGenerator;
			}
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

		protected boolean isConditionPassing(Object result) {
			if (StringUtils.hasText(this.operation.getCondition())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.condition(this.operation.getCondition(), this.method, evaluationContext);
			}
			return true;
		}

		protected boolean canPutToCache(Object value) {
			String unless = "";
			if (this.operation instanceof CacheableOperation) {
				unless = ((CacheableOperation) this.operation).getUnless();
			} else if (this.operation instanceof CachePutOperation) {
				unless = ((CachePutOperation) this.operation).getUnless();
			}
			if (StringUtils.hasText(unless)) {
				EvaluationContext evaluationContext = createEvaluationContext(value);
				return !evaluator.unless(unless, this.method, evaluationContext);
			}
			return true;
		}

		/**
		 * Computes the key for the given caching operation.
		 * @return generated key (null if none can be generated)
		 */
		protected Object generateKey(Object result) {
			if (StringUtils.hasText(this.operation.getKey())) {
				EvaluationContext evaluationContext = createEvaluationContext(result);
				return evaluator.key(this.operation.getKey(), this.method, evaluationContext);
			}
			return operationKeyGenerator.generate(this.target, this.method, this.args);
		}

		private EvaluationContext createEvaluationContext(Object result) {
			return evaluator.createEvaluationContext(
					this.caches, this.method, this.args, this.target, this.targetClass, result);
		}

		protected Collection<? extends Cache> getCaches() {
			return this.caches;
		}
	}


	private static class CachePutRequest {

		private final CacheOperationContext context;

		private final Object key;

		public CachePutRequest(CacheOperationContext context, Object key) {
			this.context = context;
			this.key = key;
		}

		public void apply(Object result) {
			if (this.context.canPutToCache(result)) {
				for (Cache cache : this.context.getCaches()) {
					cache.put(this.key, result);
				}
			}
		}
	}

}
