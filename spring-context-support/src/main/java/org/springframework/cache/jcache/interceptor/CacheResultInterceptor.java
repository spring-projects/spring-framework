/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheResult;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.Assert;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.SerializationUtils;

/**
 * Intercept methods annotated with {@link CacheResult}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
class CacheResultInterceptor extends AbstractKeyCacheInterceptor<CacheResultOperation, CacheResult> {

	public CacheResultInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	protected @Nullable Object invoke(
			CacheOperationInvocationContext<CacheResultOperation> context, CacheOperationInvoker invoker) {

		CacheResultOperation operation = context.getOperation();
		Object cacheKey = generateKey(context);

		Cache cache = resolveCache(context);
		Cache exceptionCache = resolveExceptionCache(context);

		if (!operation.isAlwaysInvoked()) {
			Cache.ValueWrapper cachedValue = doGet(cache, cacheKey);
			if (cachedValue != null) {
				return cachedValue.get();
			}
			checkForCachedException(exceptionCache, cacheKey);
		}

		try {
			Object invocationResult = invoker.invoke();
			doPut(cache, cacheKey, invocationResult);
			return invocationResult;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			cacheException(exceptionCache, operation.getExceptionTypeFilter(), cacheKey, original);
			throw ex;
		}
	}

	/**
	 * Check for a cached exception. If the exception is found, throw it directly.
	 */
	protected void checkForCachedException(@Nullable Cache exceptionCache, Object cacheKey) {
		if (exceptionCache == null) {
			return;
		}
		Cache.ValueWrapper result = doGet(exceptionCache, cacheKey);
		if (result != null) {
			Throwable ex = (Throwable) result.get();
			Assert.state(ex != null, "No exception in cache");
			throw rewriteCallStack(ex, getClass().getName(), "invoke");
		}
	}

	protected void cacheException(@Nullable Cache exceptionCache, ExceptionTypeFilter filter, Object cacheKey, Throwable ex) {
		if (exceptionCache == null) {
			return;
		}
		if (filter.match(ex.getClass())) {
			doPut(exceptionCache, cacheKey, ex);
		}
	}

	private @Nullable Cache resolveExceptionCache(CacheOperationInvocationContext<CacheResultOperation> context) {
		CacheResolver exceptionCacheResolver = context.getOperation().getExceptionCacheResolver();
		if (exceptionCacheResolver != null) {
			return extractFrom(exceptionCacheResolver.resolveCaches(context));
		}
		return null;
	}


	/**
	 * Rewrite the call stack of the specified {@code exception} so that it matches
	 * the current call stack up to (included) the specified method invocation.
	 * <p>Clone the specified exception. If the exception is not {@code serializable},
	 * the original exception is returned. If no common ancestor can be found, returns
	 * the original exception.
	 * <p>Used to make sure that a cached exception has a valid invocation context.
	 * @param exception the exception to merge with the current call stack
	 * @param className the class name of the common ancestor
	 * @param methodName the method name of the common ancestor
	 * @return a clone exception with a rewritten call stack composed of the current call
	 * stack up to (included) the common ancestor specified by the {@code className} and
	 * {@code methodName} arguments, followed by stack trace elements of the specified
	 * {@code exception} after the common ancestor.
	 */
	private static CacheOperationInvoker.ThrowableWrapper rewriteCallStack(
			Throwable exception, String className, String methodName) {

		Throwable clone = cloneException(exception);
		if (clone == null) {
			return new CacheOperationInvoker.ThrowableWrapper(exception);
		}

		StackTraceElement[] callStack = new Exception().getStackTrace();
		StackTraceElement[] cachedCallStack = exception.getStackTrace();

		int index = findCommonAncestorIndex(callStack, className, methodName);
		int cachedIndex = findCommonAncestorIndex(cachedCallStack, className, methodName);
		if (index == -1 || cachedIndex == -1) {
			return new CacheOperationInvoker.ThrowableWrapper(exception); // Cannot find common ancestor
		}
		StackTraceElement[] result = new StackTraceElement[cachedIndex + callStack.length - index];
		System.arraycopy(cachedCallStack, 0, result, 0, cachedIndex);
		System.arraycopy(callStack, index, result, cachedIndex, callStack.length - index);

		clone.setStackTrace(result);
		return new CacheOperationInvoker.ThrowableWrapper(clone);
	}

	private static <T extends Throwable> @Nullable T cloneException(T exception) {
		try {
			return SerializationUtils.clone(exception);
		}
		catch (Exception ex) {
			return null;  // exception parameter cannot be cloned
		}
	}

	private static int findCommonAncestorIndex(StackTraceElement[] callStack, String className, String methodName) {
		for (int i = 0; i < callStack.length; i++) {
			StackTraceElement element = callStack[i];
			if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
				return i;
			}
		}
		return -1;
	}

}
