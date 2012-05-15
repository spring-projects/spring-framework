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

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.AsyncExecutionChain;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles return values of type {@link Callable} and {@link DeferredResult}.
 *
 * <p>This handler does not have a defined behavior for {@code null} return
 * values and will raise an {@link IllegalArgumentException}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return Callable.class.isAssignableFrom(paramType) || DeferredResult.class.isAssignableFrom(paramType);
	}

	@SuppressWarnings("unchecked")
	public void handleReturnValue(Object returnValue,
			MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {

		Assert.notNull(returnValue, "A Callable or a DeferredValue is required");

		mavContainer.setRequestHandled(true);

		Class<?> paramType = returnType.getParameterType();
		ServletRequest servletRequest = webRequest.getNativeRequest(ServletRequest.class);
		AsyncExecutionChain chain = AsyncExecutionChain.getForCurrentRequest(servletRequest);

		if (Callable.class.isAssignableFrom(paramType)) {
			chain.setCallable((Callable<Object>) returnValue);
			chain.startCallableChainProcessing();
		}
		else if (DeferredResult.class.isAssignableFrom(paramType)) {
			chain.startDeferredResultProcessing((DeferredResult) returnValue);
		}
		else {
			// should never happen..
			Method method = returnType.getMethod();
			throw new UnsupportedOperationException("Unknown return value: " + paramType + " in method: " + method);
		}
	}

}
