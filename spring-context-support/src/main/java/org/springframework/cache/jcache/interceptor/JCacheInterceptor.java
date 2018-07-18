/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.lang.Nullable;

/**
 * AOP Alliance MethodInterceptor for declarative cache
 * management using JSR-107 caching annotations.
 *
 * <p>Derives from the {@link JCacheAspectSupport} class which
 * contains the integration with Spring's underlying caching API.
 * JCacheInterceptor simply calls the relevant superclass method.
 *
 * <p>JCacheInterceptors are thread-safe.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheInterceptor
 */
@SuppressWarnings("serial")
public class JCacheInterceptor extends JCacheAspectSupport implements MethodInterceptor, Serializable {

	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		CacheOperationInvoker aopAllianceInvoker = () -> {
			try {
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new CacheOperationInvoker.ThrowableWrapper(ex);
			}
		};

		try {
			return execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

}
