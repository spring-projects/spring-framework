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

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Resolves ServerWebExchange-related method argument values of the following types:
 * <ul>
 * <li>{@link ServerWebExchange}
 * <li>{@link ServerHttpRequest}
 * <li>{@link WebSession}
 * <li>{@link HttpMethod}
 * <li>{@link ServerHttpResponse}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerWebExchangeArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return (ServerWebExchange.class.isAssignableFrom(paramType) ||
				ServerHttpRequest.class.isAssignableFrom(paramType) ||
				ServerHttpResponse.class.isAssignableFrom(paramType) ||
				WebSession.class.isAssignableFrom(paramType) ||
				HttpMethod.class == paramType);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		Class<?> paramType = parameter.getParameterType();
		if (ServerWebExchange.class.isAssignableFrom(paramType)) {
			return Mono.just(exchange);
		}
		else if (ServerHttpRequest.class.isAssignableFrom(paramType)) {
			return Mono.just(exchange.getRequest());
		}
		else if (ServerHttpResponse.class.isAssignableFrom(paramType)) {
			return Mono.just(exchange.getResponse());
		}
		else if (WebSession.class.isAssignableFrom(paramType)) {
			return exchange.getSession().cast(Object.class);
		}
		else if (HttpMethod.class == paramType) {
			return Mono.just(exchange.getRequest().getMethod());
		}
		else {
			// should never happen...
			return Mono.error(new UnsupportedOperationException(
					"Unknown parameter type: " + paramType + " in method: " + parameter.getMethod()));
		}
	}

}
