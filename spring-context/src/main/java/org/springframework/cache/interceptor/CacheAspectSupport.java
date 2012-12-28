/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
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
 * @since 3.1
 */
public abstract class CacheAspectSupport implements InitializingBean {

	public interface Invoker {
		Object invoke();
	}

	protected final Log logger = LogFactory.getLog(getClass());

	private CacheManager cacheManager;

	private CacheOperationSource cacheOperationSource;

	private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

	private KeyGenerator keyGenerator = new DefaultKeyGenerator();

	private boolean initialized = false;

	private static final String CACHEABLE = "cacheable", UPDATE = "cacheupdate", EVICT = "cacheevict";

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
		this.cacheOperationSource =
				(cacheOperationSources.length > 1 ?
						new CompositeCacheOperationSource(cacheOperationSources) :
						cacheOperationSources[0]);
	}

	/**
	 * Return the CacheOperationSource for this cache aspect.
	 */
	public CacheOperationSource getCacheOperationSource() {
		return this.cacheOperationSource;
	}

	/**
	 * Set the KeyGenerator for this cache aspect.
	 * Default is {@link DefaultKeyGenerator}.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Return the KeyGenerator for this cache aspect,
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	public void afterPropertiesSet() {
		if (this.cacheManager == null) {
			throw new IllegalStateException("'cacheManager' is required");
		}
		if (this.cacheOperationSource == null) {
			throw new IllegalStateException("The 'cacheOperationSources' property is required: "
					+ "If there are no cacheable methods, then don't use a cache aspect.");
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

	protected Collection<Cache> getCaches(CacheOperation operation) {
		Set<String> cacheNames = operation.getCacheNames();
		Collection<Cache> caches = new ArrayList<Cache>(cacheNames.size());
		for (String cacheName : cacheNames) {
			Cache cache = this.cacheManager.getCache(cacheName);
			if (cache == null) {
				throw new IllegalArgumentException("Cannot find cache named [" + cacheName + "] for " + operation);
			}
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
		if (!this.initialized) {
			return invoker.invoke();
		}

		// get backing class
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
		if (targetClass == null && target != null) {
			targetClass = target.getClass();
		}
		final Collection<CacheOperation> cacheOp = getCacheOperationSource().getCacheOperations(method, targetClass);

		// analyze caching information
		if (!CollectionUtils.isEmpty(cacheOp)) {
			Map<String, Collection<CacheOperationContext>> ops = createOperationContext(cacheOp, method, args, target, targetClass);

			// start with evictions
			inspectBeforeCacheEvicts(ops.get(EVICT));

			// follow up with cacheable
			CacheStatus status = inspectCacheables(ops.get(CACHEABLE));

			Object retVal = null;
			Map<CacheOperationContext, Object> updates = inspectCacheUpdates(ops.get(UPDATE));

			if (status != null) {
				if (status.updateRequired) {
					updates.putAll(status.cUpdates);
				}
				// return cached object
				else {
					return status.retVal;
				}
			}

			retVal = invoker.invoke();

			inspectAfterCacheEvicts(ops.get(EVICT));

			if (!updates.isEmpty()) {
				update(updates, retVal);
			}

			return retVal;
		}

		return invoker.invoke();
	}

	private void inspectBeforeCacheEvicts(Collection<CacheOperationContext> evictions) {
		inspectCacheEvicts(evictions, true);
	}

	private void inspectAfterCacheEvicts(Collection<CacheOperationContext> evictions) {
		inspectCacheEvicts(evictions, false);
	}

	private void inspectCacheEvicts(Collection<CacheOperationContext> evictions, boolean beforeInvocation) {

		if (!evictions.isEmpty()) {

			boolean log = logger.isTraceEnabled();

			for (CacheOperationContext context : evictions) {
				CacheEvictOperation evictOp = (CacheEvictOperation) context.operation;

				if (beforeInvocation == evictOp.isBeforeInvocation()) {
					if (context.isConditionPassing()) {
						// for each cache
						// lazy key initialization
						Object key = null;

						for (Cache cache : context.getCaches()) {
							// cache-wide flush
							if (evictOp.isCacheWide()) {
								cache.clear();
								if (log) {
									logger.trace("Invalidating entire cache for operation " + evictOp + " on method " + context.method);
								}
							} else {
								// check key
								if (key == null) {
									key = context.generateKey();
								}
								if (log) {
									logger.trace("Invalidating cache key " + key + " for operation " + evictOp + " on method " + context.method);
								}
								cache.evict(key);
							}
						}
					} else {
						if (log) {
							logger.trace("Cache condition failed on method " + context.method + " for operation " + context.operation);
						}
					}
				}
			}
		}
	}

	private CacheStatus inspectCacheables(Collection<CacheOperationContext> cacheables) {
		Map<CacheOperationContext, Object> cUpdates = new LinkedHashMap<CacheOperationContext, Object>(cacheables.size());

		boolean updateRequire = false;
		Object retVal = null;

		if (!cacheables.isEmpty()) {
			boolean log = logger.isTraceEnabled();
			boolean atLeastOnePassed = false;

			for (CacheOperationContext context : cacheables) {
				if (context.isConditionPassing()) {
					atLeastOnePassed = true;
					Object key = context.generateKey();

					if (log) {
						logger.trace("Computed cache key " + key + " for operation " + context.operation);
					}
					if (key == null) {
						throw new IllegalArgumentException(
								"Null key returned for cache operation (maybe you are using named params on classes without debug info?) "
										+ context.operation);
					}

					// add op/key (in case an update is discovered later on)
					cUpdates.put(context, key);

					boolean localCacheHit = false;

					// check whether the cache needs to be inspected or not (the method will be invoked anyway)
					if (!updateRequire) {
						for (Cache cache : context.getCaches()) {
							Cache.ValueWrapper wrapper = cache.get(key);
							if (wrapper != null) {
								retVal = wrapper.get();
								localCacheHit = true;
								break;
							}
						}
					}

					if (!localCacheHit) {
						updateRequire = true;
					}
				}
				else {
					if (log) {
						logger.trace("Cache condition failed on method " + context.method + " for operation " + context.operation);
					}
				}
			}

			// return a status only if at least on cacheable matched
			if (atLeastOnePassed) {
				return new CacheStatus(cUpdates, updateRequire, retVal);
			}
		}

		return null;
	}

	private static class CacheStatus {
		// caches/key
		final Map<CacheOperationContext, Object> cUpdates;
		final boolean updateRequired;
		final Object retVal;

		CacheStatus(Map<CacheOperationContext, Object> cUpdates, boolean updateRequired, Object retVal) {
			this.cUpdates = cUpdates;
			this.updateRequired = updateRequired;
			this.retVal = retVal;
		}
	}

	private Map<CacheOperationContext, Object> inspectCacheUpdates(Collection<CacheOperationContext> updates) {

		Map<CacheOperationContext, Object> cUpdates = new LinkedHashMap<CacheOperationContext, Object>(updates.size());

		if (!updates.isEmpty()) {
			boolean log = logger.isTraceEnabled();

			for (CacheOperationContext context : updates) {
				if (context.isConditionPassing()) {

					Object key = context.generateKey();

					if (log) {
						logger.trace("Computed cache key " + key + " for operation " + context.operation);
					}
					if (key == null) {
						throw new IllegalArgumentException(
								"Null key returned for cache operation (maybe you are using named params on classes without debug info?) "
										+ context.operation);
					}

					// add op/key (in case an update is discovered later on)
					cUpdates.put(context, key);
				}
				else {
					if (log) {
						logger.trace("Cache condition failed on method " + context.method + " for operation " + context.operation);
					}
				}
			}
		}

		return cUpdates;
	}

	private void update(Map<CacheOperationContext, Object> updates, Object retVal) {
		for (Map.Entry<CacheOperationContext, Object> entry : updates.entrySet()) {
			for (Cache cache : entry.getKey().getCaches()) {
				cache.put(entry.getValue(), retVal);
			}
		}
	}

	private Map<String, Collection<CacheOperationContext>> createOperationContext(Collection<CacheOperation> cacheOp,
			Method method, Object[] args, Object target, Class<?> targetClass) {
		Map<String, Collection<CacheOperationContext>> map = new LinkedHashMap<String, Collection<CacheOperationContext>>(3);

		Collection<CacheOperationContext> cacheables = new ArrayList<CacheOperationContext>();
		Collection<CacheOperationContext> evicts = new ArrayList<CacheOperationContext>();
		Collection<CacheOperationContext> updates = new ArrayList<CacheOperationContext>();

		for (CacheOperation cacheOperation : cacheOp) {
			CacheOperationContext opContext = getOperationContext(cacheOperation, method, args, target, targetClass);

			if (cacheOperation instanceof CacheableOperation) {
				cacheables.add(opContext);
			}

			if (cacheOperation instanceof CacheEvictOperation) {
				evicts.add(opContext);
			}

			if (cacheOperation instanceof CachePutOperation) {
				updates.add(opContext);
			}
		}

		map.put(CACHEABLE, cacheables);
		map.put(EVICT, evicts);
		map.put(UPDATE, updates);

		return map;
	}

	protected class CacheOperationContext {

		private final CacheOperation operation;

		private final Collection<Cache> caches;

		private final Object target;

		private final Method method;

		private final Object[] args;

		// context passed around to avoid multiple creations
		private final EvaluationContext evalContext;

		public CacheOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
			this.operation = operation;
			this.caches = CacheAspectSupport.this.getCaches(operation);
			this.target = target;
			this.method = method;
			this.args = args;

			this.evalContext = evaluator.createEvaluationContext(caches, method, args, target, targetClass);
		}

		protected boolean isConditionPassing() {
			if (StringUtils.hasText(this.operation.getCondition())) {
				return evaluator.condition(this.operation.getCondition(), this.method, this.evalContext);
			}
			return true;
		}

		/**
		 * Computes the key for the given caching operation.
		 * @return generated key (null if none can be generated)
		 */
		protected Object generateKey() {
			if (StringUtils.hasText(this.operation.getKey())) {
				return evaluator.key(this.operation.getKey(), this.method, this.evalContext);
			}
			return keyGenerator.generate(this.target, this.method, this.args);
		}

		protected Collection<Cache> getCaches() {
			return this.caches;
		}
	}
}