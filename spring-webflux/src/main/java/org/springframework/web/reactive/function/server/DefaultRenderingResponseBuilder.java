/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Conventions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link RenderingResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
final class DefaultRenderingResponseBuilder implements RenderingResponse.Builder {

	private final String name;

	private int status = HttpStatus.OK.value();

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> model = new LinkedHashMap<>();


	public DefaultRenderingResponseBuilder(RenderingResponse other) {
		Assert.notNull(other, "RenderingResponse must not be null");
		this.name = other.name();
		this.status = (other instanceof DefaultRenderingResponse ?
				((DefaultRenderingResponse) other).statusCode : other.statusCode().value());
		this.headers.putAll(other.headers());
		this.model.putAll(other.model());
	}

	public DefaultRenderingResponseBuilder(String name) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
	}


	@Override
	public RenderingResponse.Builder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status.value();
		return this;
	}

	@Override
	public RenderingResponse.Builder status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public RenderingResponse.Builder cookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public RenderingResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttribute(Object attribute) {
		Assert.notNull(attribute, "Attribute must not be null");
		if (attribute instanceof Collection && ((Collection<?>) attribute).isEmpty()) {
			return this;
		}
		return modelAttribute(Conventions.getVariableName(attribute), attribute);
	}

	@Override
	public RenderingResponse.Builder modelAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.model.put(name, value);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Object... attributes) {
		modelAttributes(Arrays.asList(attributes));
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Collection<?> attributes) {
		attributes.forEach(this::modelAttribute);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Map<String, ?> attributes) {
		this.model.putAll(attributes);
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
		this.headers.putAll(headers);
		return this;
	}

	@Override
	public Mono<RenderingResponse> build() {
		return Mono.just(
				new DefaultRenderingResponse(this.status, this.headers, this.cookies, this.name, this.model));
	}


	private static final class DefaultRenderingResponse extends DefaultServerResponseBuilder.AbstractServerResponse
			implements RenderingResponse {

		private final String name;

		private final Map<String, Object> model;

		public DefaultRenderingResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies, String name, Map<String, Object> model) {

			super(statusCode, headers, cookies);
			this.name = name;
			this.model = Collections.unmodifiableMap(new LinkedHashMap<>(model));
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
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			MediaType contentType = exchange.getResponse().getHeaders().getContentType();
			Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
			Stream<ViewResolver> viewResolverStream = context.viewResolvers().stream();

			return Flux.fromStream(viewResolverStream)
					.concatMap(viewResolver -> viewResolver.resolveViewName(name(), locale))
					.next()
					.switchIfEmpty(Mono.defer(() -> {
						String error = "Could not resolve view with name '" + name() + "'";
						return Mono.error(new IllegalArgumentException(error));
					}))
					.flatMap(view -> {
						List<MediaType> mediaTypes = view.getSupportedMediaTypes();
						return view.render(model(),
								contentType == null && !mediaTypes.isEmpty() ? mediaTypes.get(0) : contentType,
								exchange);
					});
		}

	}

}
