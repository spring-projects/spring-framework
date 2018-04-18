/*
 * Copyright 2002-2017 the original author or authors.
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;

/**
 * Base {@code MethodInterceptor} implementation for tracing.
 *
 * <p>By default, log messages are written to the log for the interceptor class,
 * not the class which is being intercepted. Setting the {@code useDynamicLogger}
 * bean property to {@code true} causes all log messages to be written to
 * the {@code Log} for the target class being intercepted.
 *
 * <p>Subclasses must implement the {@code invokeUnderTrace} method, which
 * is invoked by this class ONLY when a particular invocation SHOULD be traced.
 * Subclasses should write to the {@code Log} instance provided.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setUseDynamicLogger
 * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.apache.commons.logging.Log)
 */
@SuppressWarnings("serial")
public abstract class AbstractTraceInterceptor implements MethodInterceptor, Serializable {

	/**
	 * The default {@code Log} instance used to write trace messages.
	 * This instance is mapped to the implementing {@code Class}.
	 */
	@Nullable
	protected transient Log defaultLogger = LogFactory.getLog(getClass());

	/**
	 * Indicates whether or not proxy class names should be hidden when using dynamic loggers.
	 * @see #setUseDynamicLogger
	 */
	private boolean hideProxyClassNames = false;

	/**
	 * Indicates whether to pass an exception to the logger.
	 * @see #writeToLog(Log, String, Throwable)
	 */
	private boolean logExceptionStackTrace = true;


	/**
	 * Set whether to use a dynamic logger or a static logger.
	 * Default is a static logger for this trace interceptor.
	 * <p>Used to determine which {@code Log} instance should be used to write
	 * log messages for a particular method invocation: a dynamic one for the
	 * {@code Class} getting called, or a static one for the {@code Class}
	 * of the trace interceptor.
	 * <p><b>NOTE:</b> Specify either this property or "loggerName", not both.
	 * @see #getLoggerForInvocation(org.aopalliance.intercept.MethodInvocation)
	 */
	public void setUseDynamicLogger(boolean useDynamicLogger) {
		// Release default logger if it is not being used.
		this.defaultLogger = (useDynamicLogger ? null : LogFactory.getLog(getClass()));
	}

	/**
	 * Set the name of the logger to use. The name will be passed to the
	 * underlying logger implementation through Commons Logging, getting
	 * interpreted as log category according to the logger's configuration.
	 * <p>This can be specified to not log into the category of a class
	 * (whether this interceptor's class or the class getting called)
	 * but rather into a specific named category.
	 * <p><b>NOTE:</b> Specify either this property or "useDynamicLogger", not both.
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setLoggerName(String loggerName) {
		this.defaultLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * Set to "true" to have {@link #setUseDynamicLogger dynamic loggers} hide
	 * proxy class names wherever possible. Default is "false".
	 */
	public void setHideProxyClassNames(boolean hideProxyClassNames) {
		this.hideProxyClassNames = hideProxyClassNames;
	}

	/**
	 * Set whether to pass an exception to the logger, suggesting inclusion
	 * of its stack trace into the log. Default is "true"; set this to "false"
	 * in order to reduce the log output to just the trace message (which may
	 * include the exception class name and exception message, if applicable).
	 * @since 4.3.10
	 */
	public void setLogExceptionStackTrace(boolean logExceptionStackTrace) {
		this.logExceptionStackTrace = logExceptionStackTrace;
	}


	/**
	 * Determines whether or not logging is enabled for the particular {@code MethodInvocation}.
	 * If not, the method invocation proceeds as normal, otherwise the method invocation is passed
	 * to the {@code invokeUnderTrace} method for handling.
	 * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.apache.commons.logging.Log)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Log logger = getLoggerForInvocation(invocation);
		if (isInterceptorEnabled(invocation, logger)) {
			return invokeUnderTrace(invocation, logger);
		}
		else {
			return invocation.proceed();
		}
	}

	/**
	 * Return the appropriate {@code Log} instance to use for the given
	 * {@code MethodInvocation}. If the {@code useDynamicLogger} flag
	 * is set, the {@code Log} instance will be for the target class of the
	 * {@code MethodInvocation}, otherwise the {@code Log} will be the
	 * default static logger.
	 * @param invocation the {@code MethodInvocation} being traced
	 * @return the {@code Log} instance to use
	 * @see #setUseDynamicLogger
	 */
	protected Log getLoggerForInvocation(MethodInvocation invocation) {
		if (this.defaultLogger != null) {
			return this.defaultLogger;
		}
		else {
			Object target = invocation.getThis();
			return LogFactory.getLog(getClassForLogging(target));
		}
	}

	/**
	 * Determine the class to use for logging purposes.
	 * @param target the target object to introspect
	 * @return the target class for the given object
	 * @see #setHideProxyClassNames
	 */
	protected Class<?> getClassForLogging(Object target) {
		return (this.hideProxyClassNames ? AopUtils.getTargetClass(target) : target.getClass());
	}

	/**
	 * Determine whether the interceptor should kick in, that is,
	 * whether the {@code invokeUnderTrace} method should be called.
	 * <p>Default behavior is to check whether the given {@code Log}
	 * instance is enabled. Subclasses can override this to apply the
	 * interceptor in other cases as well.
	 * @param invocation the {@code MethodInvocation} being traced
	 * @param logger the {@code Log} instance to check
	 * @see #invokeUnderTrace
	 * @see #isLogEnabled
	 */
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return isLogEnabled(logger);
	}

	/**
	 * Determine whether the given {@link Log} instance is enabled.
	 * <p>Default is {@code true} when the "trace" level is enabled.
	 * Subclasses can override this to change the level under which 'tracing' occurs.
	 * @param logger the {@code Log} instance to check
	 */
	protected boolean isLogEnabled(Log logger) {
		return logger.isTraceEnabled();
	}

	/**
	 * Write the supplied trace message to the supplied {@code Log} instance.
	 * <p>To be called by {@link #invokeUnderTrace} for enter/exit messages.
	 * <p>Delegates to {@link #writeToLog(Log, String, Throwable)} as the
	 * ultimate delegate that controls the underlying logger invocation.
	 * @since 4.3.10
	 * @see #writeToLog(Log, String, Throwable)
	 */
	protected void writeToLog(Log logger, String message) {
		writeToLog(logger, message, null);
	}

	/**
	 * Write the supplied trace message and {@link Throwable} to the
	 * supplied {@code Log} instance.
	 * <p>To be called by {@link #invokeUnderTrace} for enter/exit outcomes,
	 * potentially including an exception. Note that an exception's stack trace
	 * won't get logged when {@link #setLogExceptionStackTrace} is "false".
	 * <p>By default messages are written at {@code TRACE} level. Subclasses
	 * can override this method to control which level the message is written
	 * at, typically also overriding {@link #isLogEnabled} accordingly.
	 * @since 4.3.10
	 * @see #setLogExceptionStackTrace
	 * @see #isLogEnabled
	 */
	protected void writeToLog(Log logger, String message, @Nullable Throwable ex) {
		if (ex != null && this.logExceptionStackTrace) {
			logger.trace(message, ex);
		}
		else {
			logger.trace(message);
		}
	}


	/**
	 * Subclasses must override this method to perform any tracing around the
	 * supplied {@code MethodInvocation}. Subclasses are responsible for
	 * ensuring that the {@code MethodInvocation} actually executes by
	 * calling {@code MethodInvocation.proceed()}.
	 * <p>By default, the passed-in {@code Log} instance will have log level
	 * "trace" enabled. Subclasses do not have to check for this again, unless
	 * they overwrite the {@code isInterceptorEnabled} method to modify
	 * the default behavior, and may delegate to {@code writeToLog} for actual
	 * messages to be written.
	 * @param logger the {@code Log} to write trace messages to
	 * @return the result of the call to {@code MethodInvocation.proceed()}
	 * @throws Throwable if the call to {@code MethodInvocation.proceed()}
	 * encountered any errors
	 * @see #isLogEnabled
	 * @see #writeToLog(Log, String)
	 * @see #writeToLog(Log, String, Throwable)
	 */
	protected abstract Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable;

}
