/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Abstract aspect that routes selected methods asynchronously.
 *
 * <p>This aspect needs to be injected with an implementation of
 * {@link Executor} to activate it for a specific thread pool.
 * Otherwise it will simply delegate all calls synchronously.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0.5
 */
public abstract aspect AbstractAsyncExecutionAspect extends AsyncExecutionAspectSupport {

	/**
	 * Create an {@code AnnotationAsyncExecutionAspect} with a {@code null} default
	 * executor, which should instead be set via {@code #aspectOf} and
	 * {@link #setExecutor(Executor)}.
	 */
	public AbstractAsyncExecutionAspect() {
		super(null);
	}

	/**
	 * Apply around advice to methods matching the {@link #asyncMethod()} pointcut,
	 * submit the actual calling of the method to the correct task executor and return
	 * immediately to the caller.
	 * @return {@link Future} if the original method returns {@code Future}; {@code null}
	 * otherwise.
	 */
	Object around() : asyncMethod() {
		MethodSignature methodSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();
		AsyncTaskExecutor executor = determineAsyncExecutor(methodSignature.getMethod());
		if (executor == null) {
			return proceed();
		}
		Callable<Object> callable = new Callable<Object>() {
			public Object call() throws Exception {
				Object result = proceed();
				if (result instanceof Future) {
					return ((Future<?>) result).get();
				}
				return null;
			}};
		Future<?> result = executor.submit(callable);
		if (Future.class.isAssignableFrom(methodSignature.getReturnType())) {
			return result;
		}
		else {
			return null;
		}
	}

	/**
	 * Return the set of joinpoints at which async advice should be applied.
	 */
	public abstract pointcut asyncMethod();

}
