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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * Assists with the invocation of {@link DeferredResultProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class DeferredResultInterceptorChain {

	private static final Log logger = LogFactory.getLog(DeferredResultInterceptorChain.class);

	private final List<DeferredResultProcessingInterceptor> interceptors;

	private int preProcessingIndex = -1;


	public DeferredResultInterceptorChain(List<DeferredResultProcessingInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public void applyBeforeConcurrentHandling(NativeWebRequest request, DeferredResult<?> deferredResult)
			throws Exception {

		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			interceptor.beforeConcurrentHandling(request, deferredResult);
		}
	}

	public void applyPreProcess(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			interceptor.preProcess(request, deferredResult);
			this.preProcessingIndex++;
		}
	}

	public @Nullable Object applyPostProcess(NativeWebRequest request, DeferredResult<?> deferredResult,
			@Nullable Object concurrentResult) {

		try {
			for (int i = this.preProcessingIndex; i >= 0; i--) {
				this.interceptors.get(i).postProcess(request, deferredResult, concurrentResult);
			}
		}
		catch (Throwable ex) {
			return ex;
		}
		return concurrentResult;
	}

	public void triggerAfterTimeout(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			if (deferredResult.isSetOrExpired()) {
				return;
			}
			if (!interceptor.handleTimeout(request, deferredResult)){
				break;
			}
		}
	}

	/**
	 * Determine if further error handling should be bypassed.
	 * @return {@code true} to continue error handling, or false to bypass any further
	 * error handling
	 */
	public boolean triggerAfterError(NativeWebRequest request, DeferredResult<?> deferredResult, Throwable ex)
			throws Exception {

		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			if (deferredResult.isSetOrExpired()) {
				return false;
			}
			if (!interceptor.handleError(request, deferredResult, ex)){
				return false;
			}
		}
		return true;
	}

	public void triggerAfterCompletion(NativeWebRequest request, DeferredResult<?> deferredResult) {
		for (int i = this.preProcessingIndex; i >= 0; i--) {
			try {
				this.interceptors.get(i).afterCompletion(request, deferredResult);
			}
			catch (Throwable ex) {
				logger.trace("Ignoring failure in afterCompletion method", ex);
			}
		}
	}

}
