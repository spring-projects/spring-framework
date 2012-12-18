/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * Interceptor that throttles concurrent access, blocking invocations
 * if a specified concurrency limit is reached.
 *
 * <p>Can be applied to methods of local services that involve heavy use
 * of system resources, in a scenario where it is more efficient to
 * throttle concurrency for a specific service rather than restricting
 * the entire thread pool (e.g. the web container's thread pool).
 *
 * <p>The default concurrency limit of this interceptor is 1.
 * Specify the "concurrencyLimit" bean property to change this value.
 *
 * @author Juergen Hoeller
 * @since 11.02.2004
 * @see #setConcurrencyLimit
 */
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport
		implements MethodInterceptor, Serializable {

	public ConcurrencyThrottleInterceptor() {
		setConcurrencyLimit(1);
	}

	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		beforeAccess();
		try {
			return methodInvocation.proceed();
		}
		finally {
			afterAccess();
		}
	}

}
