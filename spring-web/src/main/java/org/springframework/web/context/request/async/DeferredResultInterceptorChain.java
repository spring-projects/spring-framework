/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	public void applyBeforeConcurrentHandling(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
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

	public Object applyPostProcess(NativeWebRequest request,  DeferredResult<?> deferredResult, Object concurrentResult) {
		try {
			for (int i = this.preProcessingIndex; i >= 0; i--) {
				this.interceptors.get(i).postProcess(request, deferredResult, concurrentResult);
			}
		}
		catch (Throwable t) {
			return t;
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

	public void triggerAfterCompletion(NativeWebRequest request, DeferredResult<?> deferredResult) {
		for (int i = this.preProcessingIndex; i >= 0; i--) {
			try {
				this.interceptors.get(i).afterCompletion(request, deferredResult);
			}
			catch (Throwable t) {
				logger.error("afterCompletion error", t);
			}
		}
	}

}
