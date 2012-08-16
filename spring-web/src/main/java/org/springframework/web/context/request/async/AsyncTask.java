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
package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * Holder for a {@link Callable}, a timeout value, and a task executor.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncTask {

	private final Callable<?> callable;

	private final Long timeout;

	private final String executorName;

	private final AsyncTaskExecutor executor;

	private BeanFactory beanFactory;


	/**
	 * Create an AsyncTask with a timeout value and a Callable.
	 * @param timeout timeout value in milliseconds
	 * @param callable the callable for concurrent handling
	 */
	public AsyncTask(long timeout, Callable<?> callable) {
		this(timeout, null, null, callable);
		Assert.notNull(timeout, "Timeout must not be null");
	}

	/**
	 * Create an AsyncTask with a timeout value, an executor name, and a Callable.
	 * @param timeout timeout value in milliseconds; ignored if {@code null}
	 * @param callable the callable for concurrent handling
	 */
	public AsyncTask(Long timeout, String executorName, Callable<?> callable) {
		this(timeout, null, executorName, callable);
		Assert.notNull(executor, "Executor name must not be null");
	}

	/**
	 * Create an AsyncTask with a timeout value, an executor instance, and a Callable.
	 * @param timeout timeout value in milliseconds; ignored if {@code null}
	 * @param callable the callable for concurrent handling
	 */
	public AsyncTask(Long timeout, AsyncTaskExecutor executor, Callable<?> callable) {
		this(timeout, executor, null, callable);
		Assert.notNull(executor, "Executor must not be null");
	}

	private AsyncTask(Long timeout, AsyncTaskExecutor executor, String executorName, Callable<?> callable) {
		Assert.notNull(callable, "Callable must not be null");
		this.callable = callable;
		this.timeout = timeout;
		this.executor = executor;
		this.executorName = executorName;
	}


	/**
	 * Return the {@link Callable} to use for concurrent handling, never {@code null}.
	 */
	public Callable<?> getCallable() {
		return this.callable;
	}

	/**
	 * Return the timeout value in milliseconds or {@code null} if not value is set.
	 */
	public Long getTimeout() {
		return this.timeout;
	}

	/**
	 * Return the AsyncTaskExecutor to use for concurrent handling, or {@code null}.
	 */
	public AsyncTaskExecutor getExecutor() {
		if (this.executor != null) {
			return this.executor;
		}
		else if (this.executorName != null) {
			Assert.state(this.beanFactory != null, "A BeanFactory is required to look up an task executor bean");
			return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
		}
		else {
			return null;
		}
	}

	/**
	 * A {@link BeanFactory} to use to resolve an executor name. Applications are
	 * not expected to have to set this property when AsyncTask is used in a
	 * Spring MVC controller.
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

}
