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

package org.springframework.cache.aspectj;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheOperationSource;

/**
 * Abstract superaspect for AspectJ cache aspects. Concrete
 * subaspects will implement the <code>cacheMethodExecution()</code>
 * pointcut using a strategy such as Java 5 annotations.
 *
 * <p>Suitable for use inside or outside the Spring IoC container.
 * Set the "cacheManager" property appropriately, allowing
 * use of any cache implementation supported by Spring.
 *
 * <p><b>NB:</b> If a method implements an interface that is itself
 * cache annotated, the relevant Spring cache definition
 * will <i>not</i> be resolved.
 *
 * @author Costin Leau
 * @since 3.1
 */
public abstract aspect AbstractCacheAspect extends CacheAspectSupport {

	protected AbstractCacheAspect() {
	}

	/**
	 * Construct object using the given caching metadata retrieval strategy.
	 * @param cos {@link CacheOperationSource} implementation, retrieving Spring
	 * cache metadata for each joinpoint.
	 */
	protected AbstractCacheAspect(CacheOperationSource... cos) {
		setCacheOperationSources(cos);
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();

		Callable<Object> ajInvocation = new Callable<Object>() {
			public Object call() {
				return proceed(cachedObject);
			}
		};

		try{
			return execute(ajInvocation, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());	
		} catch (Exception ex){
			throw new RuntimeException("Cannot cache target ", ex);
		}
	}

	/**
	 * Concrete subaspects must implement this pointcut, to identify
	 * cached methods. For each selected joinpoint, {@link CacheOperationDefinition}
	 * will be retrieved using Spring's {@link CacheOperationSource} interface.
	 */
	protected abstract pointcut cacheMethodExecution(Object cachedObject);

}
