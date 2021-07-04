/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP Alliance {@code MethodInterceptor} that can be introduced in a chain
 * to display verbose information about intercepted invocations to the logger.
 *
 * <p>Logs full invocation details on method entry and method exit,
 * including invocation arguments and invocation count. This is only
 * intended for debugging purposes; use {@code SimpleTraceInterceptor}
 * or {@code CustomizableTraceInterceptor} for pure tracing purposes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleTraceInterceptor
 * @see CustomizableTraceInterceptor
 */
@SuppressWarnings("serial")
public class DebugInterceptor extends SimpleTraceInterceptor {

	private volatile long count;


	/**
	 * Create a new DebugInterceptor with a static logger.
	 */
	public DebugInterceptor() {
	}

	/**
	 * Create a new DebugInterceptor with dynamic or static logger,
	 * according to the given flag.
	 * @param useDynamicLogger whether to use a dynamic logger or a static logger
	 * @see #setUseDynamicLogger
	 */
	public DebugInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		synchronized (this) {
			this.count++;
		}
		return super.invoke(invocation);
	}

	@Override
	protected String getInvocationDescription(MethodInvocation invocation) {
		return invocation + "; count=" + this.count;
	}


	/**
	 * Return the number of times this interceptor has been invoked.
	 */
	public long getCount() {
		return this.count;
	}

	/**
	 * Reset the invocation count to zero.
	 */
	public synchronized void resetCount() {
		this.count = 0;
	}

}
