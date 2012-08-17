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
package org.springframework.web.servlet.config.annotation;

import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.context.request.async.AsyncTask;

/**
 * Helps with configuring a options for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncSupportConfigurer {

	private AsyncTaskExecutor taskExecutor;

	private Long timeout;

	/**
	 * Set the default {@link AsyncTaskExecutor} to use when a controller method
	 * returns a {@link Callable}. Controller methods can override this default on
	 * a per-request basis by returning an {@link AsyncTask}.
	 *
	 * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used and it's
	 * highly recommended to change that default in production since the simple
	 * executor does not re-use threads.
	 *
	 * @param taskExecutor the task executor instance to use by default
	 */
	public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * Specify the amount of time, in milliseconds, before asynchronous request
	 * handling times out. In Servlet 3, the timeout begins after the main request
	 * processing thread has exited and ends when the request is dispatched again
	 * for further processing of the concurrently produced result.
	 * <p>If this value is not set, the default timeout of the underlying
	 * implementation is used, e.g. 10 seconds on Tomcat with Servlet 3.
	 *
	 * @param timeout the timeout value in milliseconds
	 */
	public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	protected AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected Long getTimeout() {
		return this.timeout;
	}

}
