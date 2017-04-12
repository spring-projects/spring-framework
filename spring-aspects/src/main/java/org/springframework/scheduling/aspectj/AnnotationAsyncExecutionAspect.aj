/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scheduling.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;

/**
 * Aspect to route methods based on Spring's {@link Async} annotation.
 *
 * <p>This aspect routes methods marked with the {@link Async} annotation as well as methods
 * in classes marked with the same. Any method expected to be routed asynchronously must
 * return either {@code void}, {@link Future}, or a subtype of {@link Future} (in particular,
 * Spring's {@link org.springframework.util.concurrent.ListenableFuture}). This aspect,
 * therefore, will produce a compile-time error for methods that violate this constraint
 * on the return type. If, however, a class marked with {@code @Async} contains a method
 * that violates this constraint, it produces only a warning.
 *
 * <p>This aspect needs to be injected with an implementation of a task-oriented
 * {@link java.util.concurrent.Executor} to activate it for a specific thread pool,
 * or with a {@link org.springframework.beans.factory.BeanFactory} for default
 * executor lookup. Otherwise it will simply delegate all calls synchronously.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 3.0.5
 * @see #setExecutor
 * @see #setBeanFactory
 * @see #getDefaultExecutor
 */
public aspect AnnotationAsyncExecutionAspect extends AbstractAsyncExecutionAspect {

	private pointcut asyncMarkedMethod() : execution(@Async (void || Future+) *(..));

	private pointcut asyncTypeMarkedMethod() : execution((void || Future+) (@Async *).*(..));

	public pointcut asyncMethod() : asyncMarkedMethod() || asyncTypeMarkedMethod();


	/**
	 * This implementation inspects the given method and its declaring class for the
	 * {@code @Async} annotation, returning the qualifier value expressed by {@link Async#value()}.
	 * If {@code @Async} is specified at both the method and class level, the method's
	 * {@code #value} takes precedence (even if empty string, indicating that the default
	 * executor should be used preferentially).
	 * @return the qualifier if specified, otherwise empty string indicating that the
	 * {@linkplain #setExecutor default executor} should be used
	 * @see #determineAsyncExecutor(Method)
	 */
	@Override
	protected String getExecutorQualifier(Method method) {
		// Maintainer's note: changes made here should also be made in
		// AnnotationAsyncExecutionInterceptor#getExecutorQualifier
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}


	declare error:
		execution(@Async !(void || Future+) *(..)):
		"Only methods that return void or Future may have an @Async annotation";

	declare warning:
		execution(!(void || Future+) (@Async *).*(..)):
		"Methods in a class marked with @Async that do not return void or Future will be routed synchronously";

}
