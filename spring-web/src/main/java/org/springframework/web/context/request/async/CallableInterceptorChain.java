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
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Assists with the invocation of {@link CallableProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class CallableInterceptorChain {

	private static Log logger = LogFactory.getLog(CallableInterceptorChain.class);

	private final List<CallableProcessingInterceptor> interceptors;

	private int interceptorIndex = -1;


	public CallableInterceptorChain(Collection<CallableProcessingInterceptor> interceptors) {
		this.interceptors = new ArrayList<CallableProcessingInterceptor>(interceptors);
	}

	public void applyPreProcess(NativeWebRequest request, Callable<?> task) throws Exception {
		for (int i = 0; i < this.interceptors.size(); i++) {
			this.interceptors.get(i).preProcess(request, task);
			this.interceptorIndex = i;
		}
	}

	public void applyPostProcess(NativeWebRequest request, Callable<?> task, Object concurrentResult) {
		for (int i = this.interceptorIndex; i >= 0; i--) {
			try {
				this.interceptors.get(i).postProcess(request, task, concurrentResult);
			}
			catch (Exception ex) {
				logger.error("CallableProcessingInterceptor.postProcess threw exception", ex);
			}
		}
	}

}
