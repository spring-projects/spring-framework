/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments of type {@link HttpEntity} or {@link RequestEntity}
 * by reading the body of the request through a compatible
 * {@code HttpMessageReader}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class HttpEntityMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public HttpEntityMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
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
				new RequestEntity<@Nullable Object>(body, request.getHeaders(), request.getMethod(), request.getURI()) :
				new HttpEntity<@Nullable Object>(body, request.getHeaders()));
	}

}
