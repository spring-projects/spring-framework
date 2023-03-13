/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Intercepts concurrent request handling, where the concurrent result is
 * obtained by waiting for a {@link DeferredResult} to be set from a thread
 * chosen by the application (e.g. in response to some external event).
 *
 * <p>A {@code DeferredResultProcessingInterceptor} is invoked before the start
 * of async processing, after the {@code DeferredResult} is set as well as on
 * timeout/error, or after completing for any reason including a timeout or network
 * error.
 *
 * <p>As a general rule exceptions raised by interceptor methods will cause
 * async processing to resume by dispatching back to the container and using
 * the Exception instance as the concurrent result. Such exceptions will then
 * be processed through the {@code HandlerExceptionResolver} mechanism.
 *
 * <p>The {@link #handleTimeout(NativeWebRequest, DeferredResult) handleTimeout}
 * method can set the {@code DeferredResult} in order to resume processing.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
public interface DeferredResultProcessingInterceptor {

	/**
	 * Invoked immediately before the start of concurrent handling, in the same
	 * thread that started it. This method may be used to capture state just prior
	 * to the start of concurrent processing with the given {@code DeferredResult}.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * Invoked immediately after the start of concurrent handling, in the same
	 * thread that started it. This method may be used to detect the start of
	 * concurrent processing with the given {@code DeferredResult}.
	 * <p>The {@code DeferredResult} may have already been set, for example at
	 * the time of its creation or by another thread.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * Invoked after a {@code DeferredResult} has been set, via
	 * {@link DeferredResult#setResult(Object)} or
	 * {@link DeferredResult#setErrorResult(Object)}, and is also ready to
	 * handle the concurrent result.
	 * <p>This method may also be invoked after a timeout when the
	 * {@code DeferredResult} was created with a constructor accepting a default
	 * timeout result.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @param concurrentResult the concurrent result
	 * @throws Exception in case of errors
	 */
	default <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
			@Nullable Object concurrentResult) throws Exception {
	}

	/**
	 * Invoked from a container thread when an async request times out before
	 * the {@code DeferredResult} has been set. Implementations may invoke
	 * {@link DeferredResult#setResult(Object) setResult} or
	 * {@link DeferredResult#setErrorResult(Object) setErrorResult} to resume processing.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request; if the
	 * {@code DeferredResult} is set, then concurrent processing is resumed and
	 * subsequent interceptors are not invoked
	 * @return {@code true} if processing should continue, or {@code false} if
	 * other interceptors should not be invoked
	 * @throws Exception in case of errors
	 */
	default <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {

		return true;
	}

	/**
	 * Invoked from a container thread when an error occurred while processing an async request
	 * before the {@code DeferredResult} has been set. Implementations may invoke
	 * {@link DeferredResult#setResult(Object) setResult} or
	 * {@link DeferredResult#setErrorResult(Object) setErrorResult} to resume processing.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request; if the
	 * {@code DeferredResult} is set, then concurrent processing is resumed and
	 * subsequent interceptors are not invoked
	 * @param t the error that occurred while request processing
	 * @return {@code true} if error handling should continue, or {@code false} if
	 * other interceptors should be bypassed and not be invoked
	 * @throws Exception in case of errors
	 */
	default <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult,
			Throwable t) throws Exception {

		return true;
	}

	/**
	 * Invoked from a container thread when an async request completed for any
	 * reason including timeout and network error. This method is useful for
	 * detecting that a {@code DeferredResult} instance is no longer usable.
	 * @param request the current request
	 * @param deferredResult the DeferredResult for the current request
	 * @throws Exception in case of errors
	 */
	default <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

}
