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

import java.util.ArrayList;
import java.util.Collection;
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

	private static Log logger = LogFactory.getLog(DeferredResultInterceptorChain.class);

	private final List<DeferredResultProcessingInterceptor> interceptors;


	public DeferredResultInterceptorChain(Collection<DeferredResultProcessingInterceptor> interceptors) {
		this.interceptors = new ArrayList<DeferredResultProcessingInterceptor>(interceptors);
	}

	public void applyPreProcess(NativeWebRequest request, DeferredResult<?> task) throws Exception {
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			interceptor.preProcess(request, task);
		}
	}

	public void applyPostProcess(NativeWebRequest request, DeferredResult<?> task, Object concurrentResult) {
		for (int i = this.interceptors.size()-1; i >= 0; i--) {
			try {
				this.interceptors.get(i).postProcess(request, task, concurrentResult);
			}
			catch (Exception ex) {
				logger.error("DeferredResultProcessingInterceptor.postProcess threw exception", ex);
			}
		}
	}

	public void triggerAfterExpiration(NativeWebRequest request, DeferredResult<?> task) {
		for (int i = this.interceptors.size()-1; i >= 0; i--) {
			try {
				this.interceptors.get(i).afterExpiration(request, task);
			}
			catch (Exception ex) {
				logger.error("DeferredResultProcessingInterceptor.afterExpiration threw exception", ex);
			}
		}
	}

}
