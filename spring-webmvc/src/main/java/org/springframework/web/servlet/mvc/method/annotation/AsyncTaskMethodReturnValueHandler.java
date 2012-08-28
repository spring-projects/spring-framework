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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.AsyncTask;
import org.springframework.web.context.request.async.AsyncWebUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles return values of type {@link AsyncTask}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncTaskMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final BeanFactory beanFactory;


	public AsyncTaskMethodReturnValueHandler(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return AsyncTask.class.isAssignableFrom(paramType);
	}

	public void handleReturnValue(Object returnValue,
			MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			return;
		}

		AsyncTask asyncTask = (AsyncTask) returnValue;
		asyncTask.setBeanFactory(this.beanFactory);
		AsyncWebUtils.getAsyncManager(webRequest).startCallableProcessing(asyncTask.getCallable(), mavContainer);
	}

}
