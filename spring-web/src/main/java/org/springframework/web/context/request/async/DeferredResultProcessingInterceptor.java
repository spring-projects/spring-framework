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

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Intercepts concurrent request handling, where the concurrent result is
 * obtained by waiting for a {@link DeferredResult} to be set from a thread
 * chosen by the application (e.g. in response to some external event).
 *
 * <p>A {@code DeferredResultProcessingInterceptor} is invoked before the start of
 * asynchronous processing and either when the {@code DeferredResult} is set or
 * when when the underlying request ends, whichever comes fist.
 *
 * <p>A {@code DeferredResultProcessingInterceptor} may be registered as follows:
 * <pre>
 * DeferredResultProcessingInterceptor interceptor = ... ;
 * WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
 * asyncManager.registerDeferredResultInterceptor("key", interceptor);
 * </pre>
 *
 * <p>To register an interceptor for every request, the above can be done through
 * a {@link WebRequestInterceptor} during pre-handling.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface DeferredResultProcessingInterceptor {

	/**
	 * Invoked before the start of concurrent handling using a
	 * {@link DeferredResult}. The invocation occurs in the thread that
	 * initiated concurrent handling.
	 *
	 * @param request the current request
	 * @param deferredResult the DeferredResult instance
	 */
	void preProcess(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception;

	/**
	 * Invoked when a {@link DeferredResult} is set either with a normal value
	 * or with a {@link DeferredResult#DeferredResult(Long, Object) timeout
	 * result}. The invocation occurs in the thread that set the result.
	 * <p>
	 * If the request ends before the {@code DeferredResult} is set, then
	 * {@link #afterExpiration(NativeWebRequest, DeferredResult)} is called.
	 *
	 * @param request the current request
	 * @param deferredResult the DeferredResult that has been set
	 * @param concurrentResult the result to which the {@code DeferredResult}
	 * was set
	 */
	void postProcess(NativeWebRequest request, DeferredResult<?> deferredResult,
			Object concurrentResult) throws Exception;


	/**
	 * Invoked when a {@link DeferredResult} expires before a result has been
	 * set possibly due to a timeout or a network error. This invocation occurs
	 * in the thread where the timeout or network error notification is
	 * processed.
	 *
	 * @param request the current request
	 * @param deferredResult the DeferredResult that has been set
	 */
	void afterExpiration(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception;

}
