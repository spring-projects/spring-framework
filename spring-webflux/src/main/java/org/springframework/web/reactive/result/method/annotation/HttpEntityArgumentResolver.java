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
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.BindingContext;
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
	 */
	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers) {
		super(readers);
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry}.
	 * @param readers readers for de-serializing the request body with
	 * @param registry for adapting to other reactive types from Flux and Mono
	 */
	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return (HttpEntity.class.equals(clazz) || RequestEntity.class.equals(clazz));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		Class<?> entityType = parameter.getParameterType();

		return readBody(parameter.nested(), false, bindingContext, exchange)
				.map(body -> createEntity(body, entityType, exchange.getRequest()))
				.defaultIfEmpty(createEntity(null, entityType, exchange.getRequest()));
	}

	private Object createEntity(Object body, Class<?> entityType, ServerHttpRequest request) {
		return RequestEntity.class.equals(entityType) ?
				new RequestEntity<>(body, request.getHeaders(), request.getMethod(), request.getURI()) :
				new HttpEntity<>(body, request.getHeaders());
	}

}
