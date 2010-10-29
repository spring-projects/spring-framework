/*
 * Copyright 2010 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance MethodInterceptor for declarative cache
 * management using the common Spring caching infrastructure
 * ({@link org.springframework.cache.Cache}).
 *
 * <p>Derives from the {@link CacheAspectSupport} class which
 * contains the integration with Spring's underlying caching API.
 * CacheInterceptor simply calls the relevant superclass methods
 * in the correct order.
 *
 * <p>CacheInterceptors are thread-safe.
 * 
 * @author Costin Leau
 */
@SuppressWarnings("serial")
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

	@SuppressWarnings("unchecked")
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		Callable<Object> aopAllianceInvocation = new Callable<Object>() {

			public Object call() throws Exception {
				try {
					return invocation.proceed();
				} catch (Throwable th) {
					if (th instanceof Exception) {
						throw (Exception) th;
					}
					throw (Error) th;
				}
			}
		};

		return execute(aopAllianceInvocation, invocation.getThis(), method, invocation.getArguments());
	}
}