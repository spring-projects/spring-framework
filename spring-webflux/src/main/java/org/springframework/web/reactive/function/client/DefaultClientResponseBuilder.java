/*
 * Copyright 2002-2019 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Default implementation of {@link ClientResponse.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0.5
 */
final class DefaultClientResponseBuilder implements ClientResponse.Builder {

	private static final HttpRequest EMPTY_REQUEST = new HttpRequest() {

		private final URI empty = URI.create("");

		@Override
		public String getMethodValue() {
			return "UNKNOWN";
		}

		@Override
		public URI getURI() {
			return this.empty;
		}

		@Override
		public HttpHeaders getHeaders() {
			return HttpHeaders.EMPTY;
		}
	};


	private ExchangeStrategies strategies;

	private int statusCode = 200;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Flux<DataBuffer> body = Flux.empty();

	private HttpRequest request;


	public DefaultClientResponseBuilder(ExchangeStrategies strategies) {
		Assert.notNull(strategies, "ExchangeStrategies must not be null");
		this.strategies = strategies;
		this.request = EMPTY_REQUEST;
	}

	public DefaultClientResponseBuilder(ClientResponse other) {
		Assert.notNull(other, "ClientResponse must not be null");
		this.strategies = other.strategies();
		this.statusCode = other.rawStatusCode();
		headers(headers -> headers.addAll(other.headers().asHttpHeaders()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		if (other instanceof DefaultClientResponse) {
			this.request = ((DefaultClientResponse) other).request();
		}
		else {
			this.request = EMPTY_REQUEST;
		}
	}


	@Override
	public DefaultClientResponseBuilder statusCode(HttpStatus statusCode) {
		return rawStatusCode(statusCode.value());
	}

	@Override
	public DefaultClientResponseBuilder rawStatusCode(int statusCode) {
		Assert.isTrue(statusCode >= 100 && statusCode < 600, "StatusCode must be between 1xx and 5xx");
		this.statusCode = statusCode;
		return this;
	}

	@Override
	public ClientResponse.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ClientResponse.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public DefaultClientResponseBuilder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, ResponseCookie.from(name, value).build());
		}
		return this;
	}

	@Override
	public ClientResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ClientResponse.Builder body(Flux<DataBuffer> body) {
		Assert.notNull(body, "Body must not be null");
		releaseBody();
		this.body = body;
		return this;
	}

	@Override
	public ClientResponse.Builder body(String body) {
		Assert.notNull(body, "Body must not be null");
		releaseBody();
		DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
		this.body = Flux.just(body).
				map(s -> {
					byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
					return dataBufferFactory.wrap(bytes);
				});
		return this;
	}

	private void releaseBody() {
		this.body.subscribe(DataBufferUtils.releaseConsumer());
	}

	@Override
	public ClientResponse.Builder request(HttpRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
		return this;
	}

	@Override
	public ClientResponse build() {
		ClientHttpResponse httpResponse =
				new BuiltClientHttpResponse(this.statusCode, this.headers, this.cookies, this.body);

		// When building ClientResponse manually, the ClientRequest.logPrefix() has to be passed,
		// e.g. via ClientResponse.Builder, but this (builder) is not used currently.
		return new DefaultClientResponse(httpResponse, this.strategies, "", "", () -> this.request);
	}


	private static class BuiltClientHttpResponse implements ClientHttpResponse {

		private final int statusCode;

		private final HttpHeaders headers;

		private final MultiValueMap<String, ResponseCookie> cookies;

		private final Flux<DataBuffer> body;

		public BuiltClientHttpResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies, Flux<DataBuffer> body) {

			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);
			this.body = body;
		}

		@Override
		public HttpStatus getStatusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public int getRawStatusCode() {
			return this.statusCode;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, ResponseCookie> getCookies() {
			return this.cookies;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.body;
		}
	}

}
