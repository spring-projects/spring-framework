/*
 * Copyright 2002-2020 the original author or authors.
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
import org.apache.commons.logging.Log;

import org.springframework.util.Assert;

/**
 * Simple AOP Alliance {@code MethodInterceptor} that can be introduced
 * in a chain to display verbose trace information about intercepted method
 * invocations, with method entry and method exit info.
 *
 * <p>Consider using {@code CustomizableTraceInterceptor} for more
 * advanced needs.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 1.2
 * @see CustomizableTraceInterceptor
 */
@SuppressWarnings("serial")
public class SimpleTraceInterceptor extends AbstractTraceInterceptor {

	/**
	 * Create a new SimpleTraceInterceptor with a static logger.
	 */
	public SimpleTraceInterceptor() {
	}

	/**
	 * Create a new SimpleTraceInterceptor with dynamic or static logger,
	 * according to the given flag.
	 * @param useDynamicLogger whether to use a dynamic logger or a static logger
	 * @see #setUseDynamicLogger
	 */
	public SimpleTraceInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String invocationDescription = getInvocationDescription(invocation);
		writeToLog(logger, "Entering " + invocationDescription);
		try {
			Object rval = invocation.proceed();
			writeToLog(logger, "Exiting " + invocationDescription);
			return rval;
		}
		catch (Throwable ex) {
			writeToLog(logger, "Exception thrown in " + invocationDescription, ex);
			throw ex;
		}
	}

	/**
	 * Return a description for the given method invocation.
	 * @param invocation the invocation to describe
	 * @return the description
	 */
	protected String getInvocationDescription(MethodInvocation invocation) {
		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		String className = target.getClass().getName();
		return "method '" + invocation.getMethod().getName() + "' of class [" + className + "]";
	}

}
