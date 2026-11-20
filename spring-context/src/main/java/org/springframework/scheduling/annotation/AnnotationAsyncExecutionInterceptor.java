/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Specialization of {@link AsyncExecutionInterceptor} that delegates method execution to
 * an {@code Executor} based on the {@link Async} annotation.
 *
 * <p>Specifically designed to support use of the {@link Async#value()} executor
 * qualifier mechanism.
 *
 * <p>Supports detecting qualifier metadata via {@code @Async} at the method or
 * declaring class level. See {@link #getExecutorQualifier(Method)} for details.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1.2
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor
	 * and a simple {@link AsyncUncaughtExceptionHandler}.
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * a local executor for this interceptor will be built otherwise
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor.
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * a local executor for this interceptor will be built otherwise
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle exceptions thrown by asynchronous method executions with {@code void}
	 * return type
	 */
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * Return the qualifier or bean name of the executor to be used when executing the
	 * given method, specified via {@link Async#value} at the method or declaring
	 * class level. If {@code @Async} is specified at both the method and class level, the
	 * method's {@code value} takes precedence (even if empty string, indicating that
	 * the default executor should be used preferentially).
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise an empty string indicating that the
	 * {@linkplain #setExecutor(Executor) default executor} should be used
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	protected @Nullable String getExecutorQualifier(Method method) {
		// Maintainer's note: changes made here should also be made in
		// AnnotationAsyncExecutionAspect#getExecutorQualifier
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}

}
