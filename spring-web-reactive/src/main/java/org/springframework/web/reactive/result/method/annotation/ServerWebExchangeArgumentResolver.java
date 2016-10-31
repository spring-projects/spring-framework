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

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves ServerWebExchange-related method argument values of the following types:
 * <ul>
 * <li>{@link ServerWebExchange}
 * <li>{@link ServerHttpRequest}
 * <li>{@link HttpMethod}
 * <li>{@link ServerHttpResponse}
 * </ul>
 *
 * <p>For the {@code WebSession} see {@link WebSessionArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSessionArgumentResolver
 */
public class ServerWebExchangeArgumentResolver implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return (ServerWebExchange.class.isAssignableFrom(paramType) ||
				ServerHttpRequest.class.isAssignableFrom(paramType) ||
				ServerHttpResponse.class.isAssignableFrom(paramType) ||
				HttpMethod.class == paramType);
	}

	@Override
	public Optional<Object> resolveArgumentValue(MethodParameter parameter, BindingContext context,
			ServerWebExchange exchange) {

		Class<?> paramType = parameter.getParameterType();
		Object value;
		if (ServerWebExchange.class.isAssignableFrom(paramType)) {
			value = exchange;
		}
		else if (ServerHttpRequest.class.isAssignableFrom(paramType)) {
			value = exchange.getRequest();
		}
		else if (ServerHttpResponse.class.isAssignableFrom(paramType)) {
			value = exchange.getResponse();
		}
		else if (HttpMethod.class == paramType) {
			value = exchange.getRequest().getMethod();
		}
		else {
			// should never happen...
			throw new IllegalArgumentException(
					"Unknown parameter type: " + paramType + " in method: " + parameter.getMethod());
		}
		return Optional.of(value);
	}

}
