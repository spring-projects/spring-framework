/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments of type {@link HttpEntity} or {@link RequestEntity}
 * by reading the body of the request through a compatible
 * {@code HttpMessageReader}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpEntityArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public HttpEntityArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterTypeNoReactiveWrapper(parameter,
				type -> HttpEntity.class.equals(type) || RequestEntity.class.equals(type));
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		Class<?> entityType = parameter.getParameterType();
		return readBody(parameter.nested(), parameter, false, bindingContext, exchange)
				.map(body -> createEntity(body, entityType, exchange.getRequest()))
				.defaultIfEmpty(createEntity(null, entityType, exchange.getRequest()));
	}

	private Object createEntity(@Nullable Object body, Class<?> entityType, ServerHttpRequest request) {
		return (RequestEntity.class.equals(entityType) ?
				new RequestEntity<>(body, request.getHeaders(), request.getMethod(), request.getURI()) :
				new HttpEntity<>(body, request.getHeaders()));
	}

}
