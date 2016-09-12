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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
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
class RequestBodyAdviceChain implements RequestBodyAdvice {

	private final List<RequestBodyAdvice> requestBodyAdvice = new ArrayList<>(4);


	/**
	 * Create an instance from a list of {@code RequestBodyAdvice}.
	 */
	public RequestBodyAdviceChain(List<RequestBodyAdvice> requestBodyAdvice) {
		this.requestBodyAdvice.addAll(requestBodyAdvice);
	}

	@Override
	public boolean supports(MethodParameter param, ResolvableType type,
			Class<? extends HttpMessageReader<?>> readerType) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public Map<String, Object> getHints(MethodParameter parameter, ResolvableType targetType,
			Class<? extends HttpMessageReader<?>> readerType) {

		Map<String, Object> hints = new HashMap<>();
		this.requestBodyAdvice
				.stream()
				.filter(advice -> advice.supports(parameter, targetType, readerType))
				.forEach(advice -> hints.putAll(advice.getHints(parameter, targetType, readerType))
		);
		return hints;
	}

	@Override
	public ReactiveHttpInputMessage beforeRead(ReactiveHttpInputMessage request, MethodParameter parameter,
			ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType) {

		for (RequestBodyAdvice advice : this.requestBodyAdvice) {
			if (advice.supports(parameter, targetType, readerType)) {
				request = advice.beforeRead(request, parameter, targetType, readerType);
			}
		}
		return request;
	}

	@Override
	public Flux<Object> afterRead(Flux<Object> body, ReactiveHttpInputMessage inputMessage, MethodParameter parameter,
			ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType) {

		for (RequestBodyAdvice advice : this.requestBodyAdvice) {
			if (advice.supports(parameter, targetType, readerType)) {
				body = advice.afterRead(body, inputMessage, parameter, targetType, readerType);
			}
		}
		return body;
	}

	@Override
	public Mono<Object> afterReadMono(Mono<Object> body, ReactiveHttpInputMessage inputMessage,
			MethodParameter parameter, ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType) {

		for (RequestBodyAdvice advice : this.requestBodyAdvice) {
			if (advice.supports(parameter, targetType, readerType)) {
				body = advice.afterReadMono(body, inputMessage, parameter, targetType, readerType);
			}
		}
		return body;
	}
}
