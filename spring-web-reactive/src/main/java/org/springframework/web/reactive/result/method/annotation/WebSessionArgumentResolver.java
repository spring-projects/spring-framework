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

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Resolves method argument value of type {@link WebSession}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ServerWebExchangeArgumentResolver
 */
public class WebSessionArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (WebSession.class.isAssignableFrom(parameter.getParameterType()));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext context,
			ServerWebExchange exchange) {

		Class<?> paramType = parameter.getParameterType();
		if (WebSession.class.isAssignableFrom(paramType)) {
			return exchange.getSession().cast(Object.class);
		}
		else {
			// should never happen...
			throw new IllegalArgumentException(
					"Unknown parameter type: " + paramType + " in method: " + parameter.getMethod());
		}
	}

}
