/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Specialization of {@link AsyncExecutionInterceptor} that delegates method execution to
 * an {@code Executor} based on the {@link Async} annotation. Specifically designed to
 * support use of {@link Async#value()} executor qualification mechanism introduced in
 * Spring 3.1.2. Supports detecting qualifier metadata via {@code @Async} at the method or
 * declaring class level. See {@link #getExecutorQualifier(Method)} for details.
 *
 * @author Chris Beams
 * @since 3.1.2
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor.
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()}
	 */
	public AnnotationAsyncExecutionInterceptor(Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * Return the qualifier or bean name of the executor to be used when executing the
	 * given method, specified via {@link Async#value} at the method or declaring
	 * class level. If {@code @Async} is specified at both the method and class level, the
	 * method's {@code #value} takes precedence (even if empty string, indicating that
	 * the default executor should be used preferentially).
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise empty string indicating that the
	 * {@linkplain #setExecutor(Executor) default executor} should be used
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	protected String getExecutorQualifier(Method method) {
		// maintainer's note: changes made here should also be made in
		// AnnotationAsyncExecutionAspect#getExecutorQualifier
		Async async = AnnotationUtils.findAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}

}
