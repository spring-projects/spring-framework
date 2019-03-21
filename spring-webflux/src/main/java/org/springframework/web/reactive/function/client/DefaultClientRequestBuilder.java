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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Default implementation of {@link ClientRequest.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
final class DefaultClientRequestBuilder implements ClientRequest.Builder {

	private HttpMethod method;

	private URI url;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private BodyInserter<?, ? super ClientHttpRequest> body = BodyInserters.empty();


	public DefaultClientRequestBuilder(ClientRequest other) {
		Assert.notNull(other, "ClientRequest must not be null");
		this.method = other.method();
		this.url = other.url();
		headers(headers -> headers.addAll(other.headers()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		attributes(attributes -> attributes.putAll(other.attributes()));
		body(other.body());
	}

	public DefaultClientRequestBuilder(HttpMethod method, URI url) {
		Assert.notNull(method, "HttpMethod must not be null");
		Assert.notNull(url, "URI must not be null");
		this.method = method;
		this.url = url;
	}


	@Override
	public ClientRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.method = method;
		return this;
	}

	@Override
	public ClientRequest.Builder url(URI url) {
		Assert.notNull(url, "URI must not be null");
		this.url = url;
		return this;
	}

	@Override
	public ClientRequest.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ClientRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ClientRequest.Builder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, value);
		}
		return this;
	}

	@Override
	public ClientRequest.Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public <S, P extends Publisher<S>> ClientRequest.Builder body(P publisher, Class<S> elementClass) {
		this.body = BodyInserters.fromPublisher(publisher, elementClass);
		return this;
	}

	@Override
	public <S, P extends Publisher<S>> ClientRequest.Builder body(
			P publisher, ParameterizedTypeReference<S> typeReference) {

		this.body = BodyInserters.fromPublisher(publisher, typeReference);
		return this;
	}

	@Override
	public ClientRequest.Builder attribute(String name, Object value) {
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ClientRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		attributesConsumer.accept(this.attributes);
		return this;
	}

	@Override
	public ClientRequest.Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
		this.body = inserter;
		return this;
	}

	@Override
	public ClientRequest build() {
		return new BodyInserterRequest(this.method, this.url, this.headers, this.cookies, this.body, this.attributes);
	}


	private static class BodyInserterRequest implements ClientRequest {

		private final HttpMethod method;

		private final URI url;

		private final HttpHeaders headers;

		private final MultiValueMap<String, String> cookies;

		private final BodyInserter<?, ? super ClientHttpRequest> body;

		private final Map<String, Object> attributes;

		public BodyInserterRequest(HttpMethod method, URI url, HttpHeaders headers,
				MultiValueMap<String, String> cookies, BodyInserter<?, ? super ClientHttpRequest> body,
				Map<String, Object> attributes) {

			this.method = method;
			this.url = url;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);
			this.body = body;
			this.attributes = Collections.unmodifiableMap(attributes);
		}

		@Override
		public HttpMethod method() {
			return this.method;
		}

		@Override
		public URI url() {
			return this.url;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, String> cookies() {
			return this.cookies;
		}

		@Override
		public BodyInserter<?, ? super ClientHttpRequest> body() {
			return this.body;
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies) {
			HttpHeaders requestHeaders = request.getHeaders();
			if (!this.headers.isEmpty()) {
				this.headers.entrySet().stream()
						.filter(entry -> !requestHeaders.containsKey(entry.getKey()))
						.forEach(entry -> requestHeaders
								.put(entry.getKey(), entry.getValue()));
			}

			MultiValueMap<String, HttpCookie> requestCookies = request.getCookies();
			if (!this.cookies.isEmpty()) {
				this.cookies.forEach((name, values) -> values.forEach(value -> {
					HttpCookie cookie = new HttpCookie(name, value);
					requestCookies.add(name, cookie);
				}));
			}

			return this.body.insert(request, new BodyInserter.Context() {
				@Override
				public List<HttpMessageWriter<?>> messageWriters() {
					return strategies.messageWriters();
				}
				@Override
				public Optional<ServerHttpRequest> serverRequest() {
					return Optional.empty();
				}
				@Override
				public Map<String, Object> hints() {
					return Collections.emptyMap();
				}
			});
		}
	}

}
