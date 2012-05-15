/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * AOP Alliance <code>MethodInterceptor</code> that processes method invocations
 * asynchronously, using a given {@link org.springframework.core.task.AsyncTaskExecutor}.
 * Typically used with the {@link org.springframework.scheduling.annotation.Async} annotation.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either <code>void</code> or
 * <code>java.util.concurrent.Future</code>. In the latter case, the Future handle
 * returned from the proxy will be an actual asynchronous Future that can be used
 * to track the result of the asynchronous method execution. However, since the
 * target method needs to implement the same signature, it will have to return
 * a temporary Future handle that just passes the return value through
 * (like Spring's {@link org.springframework.scheduling.annotation.AsyncResult}
 * or EJB 3.1's <code>javax.ejb.AsyncResult</code>).
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
public class AsyncExecutionInterceptor implements MethodInterceptor, Ordered {

	private final AsyncTaskExecutor asyncExecutor;


	/**
	 * Create a new AsyncExecutionInterceptor.
	 * @param asyncExecutor the Spring AsyncTaskExecutor to delegate to
	 */
	public AsyncExecutionInterceptor(AsyncTaskExecutor asyncExecutor) {
		Assert.notNull(asyncExecutor, "TaskExecutor must not be null");
		this.asyncExecutor = asyncExecutor;
	}

	/**
	 * Create a new AsyncExecutionInterceptor.
	 * @param asyncExecutor the <code>java.util.concurrent</code> Executor
	 * to delegate to (typically a {@link java.util.concurrent.ExecutorService}
	 */
	public AsyncExecutionInterceptor(Executor asyncExecutor) {
		this.asyncExecutor = new TaskExecutorAdapter(asyncExecutor);
	}


	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Future result = this.asyncExecutor.submit(new Callable<Object>() {
			public Object call() throws Exception {
				try {
					Object result = invocation.proceed();
					if (result instanceof Future) {
						return ((Future) result).get();
					}
				}
				catch (Throwable ex) {
					ReflectionUtils.rethrowException(ex);
				}
				return null;
			}
		});
		if (Future.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
			return result;
		}
		else {
			return null;
		}
	}

	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
