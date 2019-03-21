/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.aspectj;

import java.lang.reflect.Method;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.interceptor.JCacheAspectSupport;

/**
 * Concrete AspectJ cache aspect using JSR-107 standard annotations.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class (and/or
 * methods within that class), <i>not</i> the interface (if any) that the class
 * implements. AspectJ follows Java's rule that annotations on interfaces are <i>not</i>
 * inherited.
 *
 * <p>Any method may be annotated (regardless of visibility). Annotating non-public
 * methods directly is the only way to get caching demarcation for the execution of
 * such operations.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@RequiredTypes({"org.springframework.cache.jcache.interceptor.JCacheAspectSupport", "javax.cache.annotation.CacheResult"})
public aspect JCacheCacheAspect extends JCacheAspectSupport {

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();

		CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
			public Object invoke() {
				try {
					return proceed(cachedObject);
				}
				catch (Throwable ex) {
					throw new ThrowableWrapper(ex);
				}
			}

		};

		try {
			return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			AnyThrow.throwUnchecked(th.getOriginal());
			return null; // never reached
		}
	}

	/**
	* Definition of pointcut: matched join points will have JSR-107
	* cache management applied.
	*/
	protected pointcut cacheMethodExecution(Object cachedObject) :
			(executionOfCacheResultMethod()
				|| executionOfCachePutMethod()
				|| executionOfCacheRemoveMethod()
				|| executionOfCacheRemoveAllMethod())
			&& this(cachedObject);

	/**
	 * Matches the execution of any method with the @{@link CacheResult} annotation.
	 */
	private pointcut executionOfCacheResultMethod() :
		execution(@CacheResult * *(..));

	/**
	 * Matches the execution of any method with the @{@link CachePut} annotation.
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * Matches the execution of any method with the @{@link CacheRemove} annotation.
	 */
	private pointcut executionOfCacheRemoveMethod() :
		execution(@CacheRemove * *(..));

	/**
	 * Matches the execution of any method with the @{@link CacheRemoveAll} annotation.
	 */
	private pointcut executionOfCacheRemoveAllMethod() :
		execution(@CacheRemoveAll * *(..));


}
