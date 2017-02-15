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
package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.test.util.AssertionErrors;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import static org.springframework.web.reactive.function.BodyExtractors.toDataBuffers;
import static org.springframework.web.reactive.function.BodyExtractors.toFlux;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * Default implementation of {@link WebTestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClient implements WebTestClient {

	private final WebClient webClient;

	private final Duration responseTimeout;

	private final WiretapConnectorListener connectorListener;


	DefaultWebTestClient(WebClient.Builder webClientBuilder, ClientHttpConnector connector, Duration timeout) {
		Assert.notNull(webClientBuilder, "WebClient.Builder is required");

		WiretapConnector wiretapConnector = new WiretapConnector(connector);
		webClientBuilder.clientConnector(wiretapConnector);

		this.connectorListener = new WiretapConnectorListener();
		wiretapConnector.addListener(this.connectorListener);

		this.webClient = webClientBuilder.build();
		this.responseTimeout = (timeout != null ? timeout : Duration.ofSeconds(5));
	}

	private DefaultWebTestClient(DefaultWebTestClient webTestClient, ExchangeFilterFunction filter) {
		this.webClient = webTestClient.webClient.filter(filter);
		this.connectorListener = webTestClient.connectorListener;
		this.responseTimeout = webTestClient.responseTimeout;
	}


	private Duration getTimeout() {
		return this.responseTimeout;
	}


	@Override
	public UriSpec get() {
		return toUriSpec(WebClient::get);
	}

	@Override
	public UriSpec head() {
		return toUriSpec(WebClient::head);
	}

	@Override
	public UriSpec post() {
		return toUriSpec(WebClient::post);
	}

	@Override
	public UriSpec put() {
		return toUriSpec(WebClient::put);
	}

	@Override
	public UriSpec patch() {
		return toUriSpec(WebClient::patch);
	}

	@Override
	public UriSpec delete() {
		return toUriSpec(WebClient::delete);
	}

	@Override
	public UriSpec options() {
		return toUriSpec(WebClient::options);
	}

	private UriSpec toUriSpec(Function<WebClient, WebClient.UriSpec> function) {
		return new DefaultUriSpec(function.apply(this.webClient));
	}


	@Override
	public WebTestClient filter(ExchangeFilterFunction filter) {
		return new DefaultWebTestClient(this, filter);
	}



	private class DefaultUriSpec implements UriSpec {

		private final WebClient.UriSpec uriSpec;


		DefaultUriSpec(WebClient.UriSpec spec) {
			this.uriSpec = spec;
		}

		@Override
		public HeaderSpec uri(URI uri) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uri));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Object... uriVariables) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(Function<UriBuilder, URI> uriBuilder) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriBuilder));
		}
	}

	private class DefaultHeaderSpec implements WebTestClient.HeaderSpec {

		private final WebClient.HeaderSpec headerSpec;

		private final String requestId;


		DefaultHeaderSpec(WebClient.HeaderSpec spec) {
			this.headerSpec = spec;
			this.requestId = connectorListener.registerRequestId(spec);
		}


		@Override
		public DefaultHeaderSpec header(String headerName, String... headerValues) {
			this.headerSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public DefaultHeaderSpec headers(HttpHeaders headers) {
			this.headerSpec.headers(headers);
			return this;
		}

		@Override
		public DefaultHeaderSpec accept(MediaType... acceptableMediaTypes) {
			this.headerSpec.accept(acceptableMediaTypes);
			return this;
		}

		@Override
		public DefaultHeaderSpec acceptCharset(Charset... acceptableCharsets) {
			this.headerSpec.acceptCharset(acceptableCharsets);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentType(MediaType contentType) {
			this.headerSpec.contentType(contentType);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentLength(long contentLength) {
			this.headerSpec.contentLength(contentLength);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookie(String name, String value) {
			this.headerSpec.cookie(name, value);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookies(MultiValueMap<String, String> cookies) {
			this.headerSpec.cookies(cookies);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.headerSpec.ifModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifNoneMatch(String... ifNoneMatches) {
			this.headerSpec.ifNoneMatch(ifNoneMatches);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			return new DefaultResponseSpec(this.requestId, this.headerSpec.exchange());
		}

		@Override
		public <T> ResponseSpec exchange(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			return new DefaultResponseSpec(this.requestId, this.headerSpec.exchange(inserter));
		}

		@Override
		public <T, S extends Publisher<T>> ResponseSpec exchange(S publisher, Class<T> elementClass) {
			return new DefaultResponseSpec(this.requestId, this.headerSpec.exchange(publisher, elementClass));
		}
	}

	private class DefaultResponseSpec implements ResponseSpec {

		private final String requestId;

		private final Mono<ClientResponse> responseMono;


		public DefaultResponseSpec(String requestId, Mono<ClientResponse> responseMono) {
			this.requestId = requestId;
			this.responseMono = responseMono;
		}


		@Override
		public <T> ExchangeResult<T> decodeEntity(Class<T> entityClass) {
			return decodeEntity(ResolvableType.forClass(entityClass));
		}

		@Override
		public <T> ExchangeResult<List<T>> decodeAndCollect(Class<T> elementClass) {
			return decodeAndCollect(ResolvableType.forClass(elementClass));
		}

		@Override
		public <T> ExchangeResult<Flux<T>> decodeFlux(Class<T> elementClass) {
			return decodeFlux(ResolvableType.forClass(elementClass));
		}

		@Override
		public <T> ExchangeResult<T> decodeEntity(ResolvableType elementType) {
			return this.responseMono.then(response -> {
				Mono<T> entityMono = response.body(toMono(elementType));
				return entityMono.map(entity -> createTestExchange(entity, response));
			}).block(getTimeout());
		}

		@Override
		public <T> ExchangeResult<List<T>> decodeAndCollect(ResolvableType elementType) {
			return this.responseMono.then(response -> {
				Flux<T> entityFlux = response.body(toFlux(elementType));
				return entityFlux.collectList().map(list -> createTestExchange(list, response));
			}).block(getTimeout());
		}

		@Override
		public <T> ExchangeResult<Flux<T>> decodeFlux(ResolvableType elementType) {
			return this.responseMono.map(response -> {
				Flux<T> entityFlux = response.body(toFlux(elementType));
				return createTestExchange(entityFlux, response);
			}).block(getTimeout());
		}

		@Override
		public ExchangeResult<Void> expectNoBody() {
			return this.responseMono.map(response -> {
				DataBuffer buffer = response.body(toDataBuffers()).blockFirst(getTimeout());
				AssertionErrors.assertTrue("Expected empty body", buffer == null);
				ExchangeResult<Void> exchange = createTestExchange(null, response);
				return exchange;
			}).block(getTimeout());
		}

		private <T> ExchangeResult<T> createTestExchange(T body, ClientResponse response) {
			WiretapConnector.Info wiretapInfo = connectorListener.retrieveRequest(requestId);
			ClientHttpRequest request = wiretapInfo.getRequest();
			return new ExchangeResult<T>(
					request.getMethod(), request.getURI(), request.getHeaders(),
					response.statusCode(), response.headers().asHttpHeaders(), body);
		}
	}


	private static class WiretapConnectorListener implements Consumer<WiretapConnector.Info> {

		private static final String REQUEST_ID_HEADER_NAME = "request-id";


		private final AtomicLong index = new AtomicLong();

		private final Map<String, WiretapConnector.Info> exchanges = new ConcurrentHashMap<>();


		public String registerRequestId(WebClient.HeaderSpec headerSpec) {
			String requestId = String.valueOf(this.index.incrementAndGet());
			headerSpec.header(REQUEST_ID_HEADER_NAME, requestId);
			return requestId;
		}

		@Override
		public void accept(WiretapConnector.Info info) {
			Optional.ofNullable(info.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER_NAME))
					.ifPresent(id -> this.exchanges.put(id, info));
		}

		public WiretapConnector.Info retrieveRequest(String requestId) {
			WiretapConnector.Info info = this.exchanges.remove(requestId);
			Assert.notNull(info, "No match for request-id=" + requestId);
			return info;
		}
	}

}
