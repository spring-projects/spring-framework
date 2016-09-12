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

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments of type {@link HttpEntity} or {@link RequestEntity}
 * by reading the body of the request through a compatible
 * {@code HttpMessageReader}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpEntityArgumentResolver extends AbstractMessageReaderArgumentResolver
		implements HandlerMethodArgumentResolver {


	/**
	 * Constructor with {@link HttpMessageReader}'s and a {@link Validator}.
	 * @param readers readers for de-serializing the request body with
	 * @param validator validator to validate decoded objects with
	 */
	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers, Validator validator) {
		super(readers, validator);
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry}.
	 * @param readers readers for de-serializing the request body with
	 * @param validator validator to validate decoded objects with
	 * @param adapterRegistry for adapting to other reactive types from Flux and Mono
	 */
	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers, Validator validator,
			ReactiveAdapterRegistry adapterRegistry) {

		super(readers, validator, adapterRegistry);
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry} and a list of {@link RequestBodyAdvice}.
	 * @param readers readers for de-serializing the request body with
	 * @param validator validator to validate decoded objects with
	 * @param adapterRegistry for adapting to other reactive types from Flux and Mono
	 * @param bodyAdvice body advice to customize the request
	 */
	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers, Validator validator,
			ReactiveAdapterRegistry adapterRegistry, List<RequestBodyAdvice> bodyAdvice) {

		super(readers, validator, adapterRegistry, bodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return (HttpEntity.class.equals(clazz) || RequestEntity.class.equals(clazz));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter param, ModelMap model, ServerWebExchange exchange) {

		ResolvableType entityType = ResolvableType.forMethodParameter(param);
		MethodParameter bodyParameter = new MethodParameter(param);
		bodyParameter.increaseNestingLevel();

		return readBody(bodyParameter, false, exchange)
				.map(body -> createHttpEntity(body, entityType, exchange))
				.defaultIfEmpty(createHttpEntity(null, entityType, exchange));
	}

	private Object createHttpEntity(Object body, ResolvableType entityType,
			ServerWebExchange exchange) {

		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		if (RequestEntity.class == entityType.getRawClass()) {
			return new RequestEntity<>(body, headers, request.getMethod(), request.getURI());
		}
		else {
			return new HttpEntity<>(body, headers);
		}
	}

}
