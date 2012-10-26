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

import org.springframework.web.context.request.NativeWebRequest;

/**
 * Abstract adapter class for the {@link CallableProcessingInterceptor} interface,
 * for simplified implementation of individual methods.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class CallableProcessingInterceptorAdapter implements CallableProcessingInterceptor {

	/**
	 * This implementation is empty.
	 */
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) throws Exception {
	}

	/**
	 * This implementation always returns
	 * {@link CallableProcessingInterceptor#RESULT_NONE RESULT_NONE}.
	 */
	public <T> Object afterTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
		return RESULT_NONE;
	}

	/**
	 * This implementation is empty.
	 */
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
	}

}
