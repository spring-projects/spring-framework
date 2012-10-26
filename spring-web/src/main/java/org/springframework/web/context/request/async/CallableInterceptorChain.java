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

	private int preProcessIndex = -1;


	public CallableInterceptorChain(Collection<CallableProcessingInterceptor> interceptors) {
		this.interceptors = new ArrayList<CallableProcessingInterceptor>(interceptors);
	}

	public void applyPreProcess(NativeWebRequest request, Callable<?> task) throws Exception {
		for (CallableProcessingInterceptor interceptor : this.interceptors) {
			interceptor.preProcess(request, task);
			this.preProcessIndex++;
		}
	}

	public Object applyPostProcess(NativeWebRequest request, Callable<?> task, Object concurrentResult) {
		for (int i = this.preProcessIndex; i >= 0; i--) {
			try {
				this.interceptors.get(i).postProcess(request, task, concurrentResult);
			}
			catch (Throwable t) {
				return t;
			}
		}
		return concurrentResult;
	}

	public Object triggerAfterTimeout(NativeWebRequest request, Callable<?> task) {
		for (int i = this.interceptors.size()-1; i >= 0; i--) {
			try {
				Object result = this.interceptors.get(i).afterTimeout(request, task);
				if (result != CallableProcessingInterceptor.RESULT_NONE) {
					return result;
				}
			}
			catch (Throwable t) {
				return t;
			}
		}
		return CallableProcessingInterceptor.RESULT_NONE;
	}

	public void triggerAfterCompletion(NativeWebRequest request, Callable<?> task) {
		for (int i = this.interceptors.size()-1; i >= 0; i--) {
			try {
				this.interceptors.get(i).afterCompletion(request, task);
			}
			catch (Throwable t) {
				logger.error("afterCompletion error", t);
			}
		}
	}
}
