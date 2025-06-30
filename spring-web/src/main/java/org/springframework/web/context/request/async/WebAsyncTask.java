/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Holder for a {@link Callable}, a timeout value, and a task executor.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 * @param <V> the value type
 */
public class WebAsyncTask<V> implements BeanFactoryAware {

	private final Callable<V> callable;

	private final @Nullable Long timeout;

	private final @Nullable AsyncTaskExecutor executor;

	private final @Nullable String executorName;

	private @Nullable BeanFactory beanFactory;

	private @Nullable Callable<V> timeoutCallback;

	private @Nullable Callable<V> errorCallback;

	private @Nullable Runnable completionCallback;


	/**
	 * Create a {@code WebAsyncTask} wrapping the given {@link Callable}.
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
		this.timeout = null;
		this.executor = null;
		this.executorName = null;
	}

	/**
	 * Create a {@code WebAsyncTask} with a timeout value and a {@link Callable}.
	 * @param timeout a timeout value in milliseconds
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(long timeout, Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
		this.timeout = timeout;
		this.executor = null;
		this.executorName = null;
	}

	/**
	 * Create a {@code WebAsyncTask} with a timeout value, an executor name, and a {@link Callable}.
	 * @param timeout the timeout value in milliseconds; ignored if {@code null}
	 * @param executorName the name of an executor bean to use
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(@Nullable Long timeout, String executorName, Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		Assert.notNull(executorName, "Executor name must not be null");
		this.callable = callable;
		this.timeout = timeout;
		this.executor = null;
		this.executorName = executorName;
	}

	/**
	 * Create a {@code WebAsyncTask} with a timeout value, an executor instance, and a Callable.
	 * @param timeout the timeout value in milliseconds; ignored if {@code null}
	 * @param executor the executor to use
	 * @param callable the callable for concurrent handling
	 */
	public WebAsyncTask(@Nullable Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
		Assert.notNull(callable, "Callable must not be null");
		Assert.notNull(executor, "Executor must not be null");
		this.callable = callable;
		this.timeout = timeout;
		this.executor = executor;
		this.executorName = null;
	}


	/**
	 * Return the {@link Callable} to use for concurrent handling (never {@code null}).
	 */
	public Callable<?> getCallable() {
		return this.callable;
	}

	/**
	 * Return the timeout value in milliseconds, or {@code null} if no timeout is set.
	 */
	public @Nullable Long getTimeout() {
		return this.timeout;
	}

	/**
	 * A {@link BeanFactory} to use for resolving an executor name.
	 * <p>This factory reference will automatically be set when
	 * {@code WebAsyncTask} is used within a Spring MVC controller.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the AsyncTaskExecutor to use for concurrent handling,
	 * or {@code null} if none specified.
	 */
	public @Nullable AsyncTaskExecutor getExecutor() {
		if (this.executor != null) {
			return this.executor;
		}
		else if (this.executorName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
			return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
		}
		else {
			return null;
		}
	}


	/**
	 * Register code to invoke when the async request times out.
	 * <p>This method is called from a container thread when an async request times
	 * out before the {@code Callable} has completed. The callback is executed in
	 * the same thread and therefore should return without blocking. It may return
	 * an alternative value to use, including an {@link Exception} or return
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
	 */
	public void onTimeout(Callable<V> callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * Register code to invoke for an error during async request processing.
	 * <p>This method is called from a container thread when an error occurred
	 * while processing an async request before the {@code Callable} has
	 * completed. The callback is executed in the same thread and therefore
	 * should return without blocking. It may return an alternative value to
	 * use, including an {@link Exception} or return
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
	 * @since 5.0
	 */
	public void onError(Callable<V> callback) {
		this.errorCallback = callback;
	}

	/**
	 * Register code to invoke when the async request completes.
	 * <p>This method is called from a container thread when an async request
	 * completed for any reason, including timeout and network error.
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	CallableProcessingInterceptor getInterceptor() {
		return new CallableProcessingInterceptor() {
			@Override
			public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
				return (timeoutCallback != null ? timeoutCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}
			@Override
			public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
				return (errorCallback != null ? errorCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
			}
			@Override
			public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}

}
