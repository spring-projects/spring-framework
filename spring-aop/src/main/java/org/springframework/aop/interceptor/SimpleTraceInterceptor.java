/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

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
	protected @Nullable Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
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
