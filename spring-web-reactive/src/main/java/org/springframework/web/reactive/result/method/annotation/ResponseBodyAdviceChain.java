/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.method.ControllerAdviceBean;

/**
 * Invokes {@link RequestBodyAdvice} and {@link ResponseBodyAdvice} where each
 * instance may be (and is most likely) wrapped with
 * {@link ControllerAdviceBean ControllerAdviceBean}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ResponseBodyAdviceChain implements ResponseBodyAdvice {

	private final List<ResponseBodyAdvice> responseBodyAdvice = new ArrayList<>(4);


	/**
	 * Create an instance from a list of {@code ResponseBodyAdvice}.
	 */
	public ResponseBodyAdviceChain(List<ResponseBodyAdvice> responseBodyAdvice) {
		this.responseBodyAdvice.addAll(responseBodyAdvice);
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageWriter<?>> writerType, List<String> supportedHints) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public Map<String, Object> getHints(MethodParameter returnType, ResolvableType targetType,
			Class<? extends HttpMessageWriter<?>> writerType, List<String> supportedHints) {

		Map<String, Object> hints = new HashMap<>();
		this.responseBodyAdvice
				.stream()
				.filter(advice -> advice.supports(returnType, writerType, supportedHints))
				.forEach(advice -> hints.putAll(advice.getHints(returnType, targetType, writerType, supportedHints))
		);
		return hints;
	}

	@Override
	public Publisher<Object> beforeBodyWrite(Publisher<Object> body, MethodParameter returnType, MediaType contentType,
			Class<? extends HttpMessageWriter<?>> writerType,
			ServerHttpResponse response, List<String> supportedHints) {

		for (ResponseBodyAdvice advice : this.responseBodyAdvice) {
			if (advice.supports(returnType, writerType, supportedHints)) {
				body = advice.beforeBodyWrite(body, returnType, contentType, writerType, response, supportedHints);
			}
		}
		return body;
	}

}
