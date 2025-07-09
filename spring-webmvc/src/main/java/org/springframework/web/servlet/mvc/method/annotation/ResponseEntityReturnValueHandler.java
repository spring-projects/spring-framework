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

package org.springframework.web.servlet.mvc.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handler for return values of type {@link org.springframework.http.ResponseEntity}
 * that delegates to one of the following:
 *
 * <ul>
 * <li>{@link HttpEntityMethodProcessor} for responses with a concrete body value
 * <li>{@link ResponseBodyEmitterReturnValueHandler} for responses with a body
 * that is a {@link ResponseBodyEmitter} or an async/reactive type.
 * </ul>
 *
 * <p>Use of this wrapper allows for late check in {@link #handleReturnValue} of
 * the type of the actual body value in case the method signature does not
 * provide enough information to decide via {@link #supportsReturnType}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ResponseEntityReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final HttpEntityMethodProcessor httpEntityMethodProcessor;

	private final ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler;


	public ResponseEntityReturnValueHandler(
			HttpEntityMethodProcessor httpEntityMethodProcessor,
			ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler) {

		this.httpEntityMethodProcessor = httpEntityMethodProcessor;
		this.responseBodyEmitterHandler = responseBodyEmitterHandler;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return this.httpEntityMethodProcessor.supportsReturnType(returnType);
	}

	@Override
	public void handleReturnValue(
			@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest request) throws Exception {

		if (returnValue instanceof HttpEntity<?> httpEntity) {
			Object body = httpEntity.getBody();
			if (body != null) {
				if (this.responseBodyEmitterHandler.supportsBodyType(body.getClass())) {
					this.responseBodyEmitterHandler.handleReturnValue(returnValue, returnType, mavContainer, request);
					return;
				}
			}
		}

		this.httpEntityMethodProcessor.handleReturnValue(returnValue, returnType, mavContainer, request);
	}

}
