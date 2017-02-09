/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.web.filter.reactive;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Optional;

/**
 * Reactive {@link WebFilter} that converts posted method parameters into HTTP methods,
 * retrievable via {@link ServerHttpRequest#getMethod()}. Since browsers currently only
 * support GET and POST, a common technique - used by the Prototype library, for instance -
 * is to use a normal POST with an additional hidden form field ({@code _method})
 * to pass the "real" HTTP method along. This filter reads that parameter and changes
 * the {@link ServerHttpRequest#getMethod()} return value using {@link ServerWebExchange#mutate()}.
 *
 * <p>The name of the request parameter defaults to {@code _method}, but can be
 * adapted via the {@link #setMethodParam(String) methodParam} property.
 *
 * @author Greg Turnquist
 * @since 5.0
 */
public class HiddenHttpMethodFilter implements WebFilter {

	/** Default method parameter: {@code _method} */
	public static final String DEFAULT_METHOD_PARAM = "_method";

	private String methodParam = DEFAULT_METHOD_PARAM;

	/**
	 * Set the parameter name to look for HTTP methods.
	 * @see #DEFAULT_METHOD_PARAM
	 */
	public void setMethodParam(String methodParam) {
		Assert.hasText(methodParam, "'methodParam' must not be empty");
		this.methodParam = methodParam;
	}

	/**
	 * Transform an HTTP POST into another method based on {@code methodParam}
	 *
	 * @param exchange the current server exchange
	 * @param chain provides a way to delegate to the next filter
	 * @return
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		if (exchange.getRequest().getMethod() == HttpMethod.POST) {
			return exchange.getFormData()
				.map(map -> Optional.ofNullable(map.getFirst(methodParam)))
				.map(method -> convertedRequest(exchange, method))
				.then(convertedExchange -> chain.filter(convertedExchange));
		} else {
			return chain.filter(exchange);
		}
	}

	/**
	 * Mutate exchange into a new HTTP method.
	 *
	 * @param exchange - original request
	 * @param method - request HTTP method based on form data
	 * @return a mutated {@link ServerWebExchange}
	 */
	private ServerWebExchange convertedRequest(ServerWebExchange exchange, Optional<String> method) {

		String upperMethod = method
			.map(String::toString)
			.orElse(HttpMethod.POST.toString())
			.toUpperCase(Locale.ENGLISH);

		return exchange.mutate()
			.request(builder -> builder.method(HttpMethod.resolve(upperMethod)))
			.build();
	}
}
