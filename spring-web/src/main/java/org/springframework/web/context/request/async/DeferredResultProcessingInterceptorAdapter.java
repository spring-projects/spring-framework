/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.web.context.request.NativeWebRequest;

/**
 * Abstract adapter class for the {@link DeferredResultProcessingInterceptor}
 * interface for simplified implementation of individual methods.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 * @deprecated as of 5.0 where DeferredResultProcessingInterceptor has default methods
 */
@Deprecated
public abstract class DeferredResultProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult)
			throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult,
			Object concurrentResult) throws Exception {
	}

	/**
	 * This implementation returns {@code true} by default allowing other interceptors
	 * to be given a chance to handle the timeout.
	 */
	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
		return true;
	}

	/**
	 * This implementation returns {@code true} by default allowing other interceptors
	 * to be given a chance to handle the error.
	 */
	@Override
	public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult, Throwable t)
			throws Exception {
		return true;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
	}

}
