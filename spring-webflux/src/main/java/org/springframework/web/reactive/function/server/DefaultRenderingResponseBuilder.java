/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link RenderingResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultRenderingResponseBuilder implements RenderingResponse.Builder {

	private final String name;

	private HttpStatus status = HttpStatus.OK;

	private final HttpHeaders headers = new HttpHeaders();

	private final Map<String, Object> model = new LinkedHashMap<String, Object>();


	public DefaultRenderingResponseBuilder(String name) {
		this.name = name;
	}


	@Override
	public RenderingResponse.Builder status(HttpStatus status) {
		Assert.notNull(status, "'status' must not be null");
		this.status = status;
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttribute(Object attribute) {
		Assert.notNull(attribute, "'value' must not be null");
		if (attribute instanceof Collection && ((Collection<?>) attribute).isEmpty()) {
			return this;
		}
		return modelAttribute(Conventions.getVariableName(attribute), attribute);
	}

	@Override
	public RenderingResponse.Builder modelAttribute(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		this.model.put(name, value);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Object... attributes) {
		if (attributes != null) {
			modelAttributes(Arrays.asList(attributes));
		}
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Collection<?> attributes) {
		if (attributes != null) {
			attributes.forEach(this::modelAttribute);
		}
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			this.model.putAll(attributes);
		}
		return this;
	}

	@Override
	public RenderingResponse.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public RenderingResponse.Builder headers(HttpHeaders headers) {
		if (headers != null) {
			this.headers.putAll(headers);
		}
		return this;
	}

	@Override
	public Mono<RenderingResponse> build() {
		return Mono.just(new DefaultRenderingResponse(this.status, this.headers, this.name, this.model));
	}


	private final static class DefaultRenderingResponse
			extends DefaultServerResponseBuilder.AbstractServerResponse
			implements RenderingResponse {

		private final String name;

		private final Map<String, Object> model;

		public DefaultRenderingResponse(HttpStatus statusCode, HttpHeaders headers, String name,
				Map<String, Object> model) {
			super(statusCode, headers);
			this.name = name;
			this.model = unmodifiableCopy(model);
		}

		private static <K, V> Map<K, V> unmodifiableCopy(Map<? extends K, ? extends V> m) {
			return Collections.unmodifiableMap(new LinkedHashMap<>(m));
		}


		@Override
		public String name() {
			return this.name;
		}

		@Override
		public Map<String, Object> model() {
			return this.model;
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange, HandlerStrategies strategies) {
			ServerHttpResponse response = exchange.getResponse();
			writeStatusAndHeaders(response);
			MediaType contentType = exchange.getResponse().getHeaders().getContentType();
			Locale locale = resolveLocale(exchange, strategies);
			Stream<ViewResolver> viewResolverStream = strategies.viewResolvers().get();

			return Flux.fromStream(viewResolverStream)
					.concatMap(viewResolver -> viewResolver.resolveViewName(name(), locale))
					.next()
					.otherwiseIfEmpty(Mono.error(new IllegalArgumentException("Could not resolve view with name '" +
							name() +"'")))
					.then(view -> view.render(model(), contentType, exchange));
		}

		private Locale resolveLocale(ServerWebExchange exchange, HandlerStrategies strategies) {
			ServerRequest request =
					exchange.<ServerRequest>getAttribute(RouterFunctions.REQUEST_ATTRIBUTE)
							.orElseThrow(() -> new IllegalStateException(
									"Could not find ServerRequest in exchange attributes"));

			return strategies.localeResolver().get()
					.apply(request)
					.orElse(Locale.getDefault());

		}
	}

}
