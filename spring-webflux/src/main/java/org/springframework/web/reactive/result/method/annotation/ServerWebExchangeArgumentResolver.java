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
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves ServerWebExchange-related method argument values of the following types:
 * <ul>
 * <li>{@link ServerWebExchange}
 * <li>{@link ServerHttpRequest}
 * <li>{@link ServerHttpResponse}
 * <li>{@link HttpMethod}
 * </ul>
 *
 * <p>For the {@code WebSession} see {@link WebSessionArgumentResolver}
 * and for the {@code Principal} see {@link PrincipalArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSessionArgumentResolver
 * @see PrincipalArgumentResolver
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
	public Optional<Object> resolveArgumentValue(MethodParameter methodParameter,
			BindingContext context, ServerWebExchange exchange) {

		Class<?> paramType = methodParameter.getParameterType();
		if (ServerWebExchange.class.isAssignableFrom(paramType)) {
			return Optional.of(exchange);
		}
		else if (ServerHttpRequest.class.isAssignableFrom(paramType)) {
			return Optional.of(exchange.getRequest());
		}
		else if (ServerHttpResponse.class.isAssignableFrom(paramType)) {
			return Optional.of(exchange.getResponse());
		}
		else if (HttpMethod.class == paramType) {
			return Optional.of(exchange.getRequest().getMethod());
		}
		else {
			// should never happen...
			throw new IllegalArgumentException("Unknown parameter type: " +
					paramType + " in method: " + methodParameter.getMethod());
		}
	}

}
