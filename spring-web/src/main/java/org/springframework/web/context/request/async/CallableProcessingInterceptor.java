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

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Intercepts concurrent request handling, where the concurrent result is
 * obtained by executing a {@link Callable} on behalf of the application with an
 * {@link AsyncTaskExecutor}.
 * <p>
 * A {@code CallableProcessingInterceptor} is invoked before and after the
 * invocation of the {@code Callable} task in the asynchronous thread.
 *
 * <p>A {@code CallableProcessingInterceptor} may be registered as follows:
 * <pre>
 * CallableProcessingInterceptor interceptor = ... ;
 * WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
 * asyncManager.registerCallableInterceptor("key", interceptor);
 * </pre>
 *
 * <p>To register an interceptor for every request, the above can be done through
 * a {@link WebRequestInterceptor} during pre-handling.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface CallableProcessingInterceptor {

	/**
	 * Invoked from the asynchronous thread in which the {@code Callable} is
	 * executed, before the {@code Callable} is invoked.
	 *
	 * @param request the current request
	 * @param task the task that will produce a result
	 */
	void preProcess(NativeWebRequest request, Callable<?> task) throws Exception;

	/**
	 * Invoked from the asynchronous thread in which the {@code Callable} is
	 * executed, after the {@code Callable} returned a result.
	 *
	 * @param request the current request
	 * @param task the task that produced the result
	 * @param concurrentResult the result of concurrent processing, which could
	 * be a {@link Throwable} if the {@code Callable} raised an exception
	 */
	void postProcess(NativeWebRequest request, Callable<?> task, Object concurrentResult) throws Exception;

}
