/*
 * Copyright 2010-2011 the original author or authors.
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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.KeyGenerator;
import org.springframework.cache.support.DefaultKeyGenerator;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for caching aspects, such as the {@link CacheInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring caching infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>If no caching name has been specified in the <code>CacheOperationDefinition</code>,
 * the exposed name will be the <code>fully-qualified class name + "." + method name</code>
 * (by default).
 *
 * <p>Uses the <b>Strategy</b> design pattern. A <code>CacheManager</code>
 * implementation will perform the actual transaction management, and a
 * <code>CacheDefinitionSource</code> is used for determining caching operation definitions.
 *
 * <p>A cache aspect is serializable if its <code>CacheManager</code>
 * and <code>CacheDefinitionSource</code> are serializable.
 *
 * @author Costin Leau
 */
public abstract class CacheAspectSupport implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private CacheManager cacheManager;

	private CacheOperationSource cacheDefinitionSource;

	private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

	private KeyGenerator<?> keyGenerator = new DefaultKeyGenerator();

	public void afterPropertiesSet() {
		if (this.cacheManager == null) {
			throw new IllegalStateException("Setting the property 'cacheManager' is required");
		}
		if (this.cacheDefinitionSource == null) {
			throw new IllegalStateException("Either 'cacheDefinitionSource' or 'cacheDefinitionSources' is required: "
					+ "If there are no cacheable methods, then don't use a cache aspect.");
		}
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

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public CacheOperationSource getCacheDefinitionSource() {
		return cacheDefinitionSource;
	}

	public KeyGenerator getKeyGenerator() {
		return keyGenerator;
	}

	public <K> void setKeyGenerator(KeyGenerator<K> keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Set multiple cache definition sources which are used to find the cache
	 * attributes. Will build a CompositeCachingDefinitionSource for the given sources.
	 */
	public void setCacheDefinitionSources(CacheOperationSource... cacheDefinitionSources) {
		Assert.notEmpty(cacheDefinitionSources);
		this.cacheDefinitionSource = (cacheDefinitionSources.length > 1 ? new CompositeCacheOperationSource(
				cacheDefinitionSources) : cacheDefinitionSources[0]);
	}

	protected Collection<Cache<?, ?>> getCaches(CacheOperation operation) {
		Set<String> cacheNames = operation.getCacheNames();

		Collection<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>(cacheNames.size());

		for (String cacheName : cacheNames) {
			Cache<Object, Object> cache = cacheManager.getCache(cacheName);
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

	@SuppressWarnings("unchecked")
	protected Object execute(Callable<Object> invocation, Object target, Method method, Object[] args) throws Exception {
		// get backing class
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);

		if (targetClass == null && target != null) {
			targetClass = target.getClass();
		}

		boolean log = logger.isTraceEnabled();

		final CacheOperation cacheOp = getCacheDefinitionSource().getCacheOperation(method, targetClass);

		Object retVal = null;

		// analyze caching information
		if (cacheOp != null) {
			CacheOperationContext context = getOperationContext(cacheOp, method, args, target, targetClass);
			Collection<Cache<?, ?>> caches = context.getCaches();

			if (context.hasConditionPassed()) {
				// check operation
				if (cacheOp instanceof CacheUpdateOperation) {
					Object key = context.generateKey();

					if (log) {
						logger.trace("Computed cache key " + key + " for definition " + cacheOp);
					}

					if (key == null) {
						throw new IllegalArgumentException(
								"Null key returned for cache definition (maybe you are using named params on classes without debug info?) "
										+ cacheOp);
					}

					// for each cache
					boolean cacheHit = false;

					for (Iterator<Cache<?, ?>> iterator = caches.iterator(); iterator.hasNext() && !cacheHit;) {
						Cache cache = iterator.next();
						Cache.ValueWrapper<Object> wrapper = cache.get(key);

						if (wrapper != null) {
							cacheHit = true;
							retVal = wrapper.get();
						}
					}

					if (!cacheHit) {
						if (log) {
							logger.trace("Key " + key + " NOT found in cache(s), invoking cached target method  "
									+ method);
						}
						retVal = invocation.call();

						// update all caches
						for (Cache cache : caches) {
							cache.put(key, retVal);
						}
					} else {
						if (log) {
							logger.trace("Key " + key + " found in cache, returning value " + retVal);
						}
					}
				}

				if (cacheOp instanceof CacheEvictOperation) {
					CacheEvictOperation evictOp = (CacheEvictOperation) cacheOp;
					retVal = invocation.call();

					// for each cache
					// lazy key initialization
					Object key = null;

					for (Cache cache : caches) {
						// flush the cache (ignore arguments)
						if (evictOp.isCacheWide()) {
							cache.clear();
							if (log) {
								logger.trace("Invalidating entire cache for definition " + cacheOp + " on method "
										+ method);
							}
						} else {
							// check key
							if (key == null) {
								key = context.generateKey();
							}
							if (log) {
								logger.trace("Invalidating cache key " + key + " for definition " + cacheOp
										+ " on method " + method);
							}
							cache.evict(key);
						}
					}
				}

				return retVal;
			} else {
				if (log) {
					logger.trace("Cache condition failed on method " + method + " for definition " + cacheOp);
				}
			}
		}

		return invocation.call();
	}

	protected class CacheOperationContext {

		private CacheOperation operation;
		private final Collection<Cache<?, ?>> caches;
		private final Object target;
		private final Method method;
		private final Object[] args;

		// context passed around to avoid multiple creations
		private final EvaluationContext evalContext;

		private final KeyGenerator<?> keyGenerator = CacheAspectSupport.this.keyGenerator;

		public CacheOperationContext(CacheOperation operation, Method method, Object[] args, Object target,
				Class<?> targetClass) {
			this.operation = operation;
			this.caches = CacheAspectSupport.this.getCaches(operation);
			this.target = target;
			this.method = method;
			this.args = args;

			this.evalContext = evaluator.createEvaluationContext(caches, method, args, target, targetClass);
		}

		/**
		 * Evaluates the definition condition.
		 * 
		 * @param operation
		 * @return
		 */
		protected boolean hasConditionPassed() {
			if (StringUtils.hasText(operation.getCondition())) {
				return evaluator.condition(operation.getCondition(), method, evalContext);
			}
			return true;
		}

		/**
		 * Computes the key for the given caching definition.
		 * 
		 * @param operation
		 * @param method
		 *            method being invoked
		 * @param objects
		 *            arguments passed during the method invocation
		 * @return generated key (null if none can be generated)
		 */
		protected Object generateKey() {
			if (StringUtils.hasText(operation.getKey())) {
				return evaluator.key(operation.getKey(), method, evalContext);
			}

			return keyGenerator.extract(target, method, args);
		}

		protected Collection<Cache<?, ?>> getCaches() {
			return caches;
		}
	}
}