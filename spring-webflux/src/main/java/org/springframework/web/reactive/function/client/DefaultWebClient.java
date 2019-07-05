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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
class DefaultWebClient implements WebClient {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
			new IllegalStateException("The underlying HTTP client completed without emitting a response."));


	private final ExchangeFunction exchangeFunction;

	private final UriBuilderFactory uriBuilderFactory;

	@Nullable
	private final HttpHeaders defaultHeaders;

	@Nullable
	private final MultiValueMap<String, String> defaultCookies;

	@Nullable
	private final Consumer<RequestHeadersSpec<?>> defaultRequest;

	private final DefaultWebClientBuilder builder;


	DefaultWebClient(ExchangeFunction exchangeFunction, @Nullable UriBuilderFactory factory,
			@Nullable HttpHeaders defaultHeaders, @Nullable MultiValueMap<String, String> defaultCookies,
			@Nullable Consumer<RequestHeadersSpec<?>> defaultRequest, DefaultWebClientBuilder builder) {

		this.exchangeFunction = exchangeFunction;
		this.uriBuilderFactory = (factory != null ? factory : new DefaultUriBuilderFactory());
		this.defaultHeaders = defaultHeaders;
		this.defaultCookies = defaultCookies;
		this.defaultRequest = defaultRequest;
		this.builder = builder;
	}


	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public RequestHeadersUriSpec<?> head() {
		return methodInternal(HttpMethod.HEAD);
	}

	@Override
	public RequestBodyUriSpec post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public RequestBodyUriSpec put() {
		return methodInternal(HttpMethod.PUT);
	}

	@Override
	public RequestBodyUriSpec patch() {
		return methodInternal(HttpMethod.PATCH);
	}

	@Override
	public RequestHeadersUriSpec<?> delete() {
		return methodInternal(HttpMethod.DELETE);
	}

	@Override
	public RequestHeadersUriSpec<?> options() {
		return methodInternal(HttpMethod.OPTIONS);
	}

	@Override
	public RequestBodyUriSpec method(HttpMethod httpMethod) {
		return methodInternal(httpMethod);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(httpMethod);
	}

	@Override
	public Builder mutate() {
		return new DefaultWebClientBuilder(this.builder);
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final HttpMethod httpMethod;

		@Nullable
		private URI uri;

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private MultiValueMap<String, String> cookies;

		@Nullable
		private BodyInserter<?, ? super ClientHttpRequest> inserter;

		private final Map<String, Object> attributes = new LinkedHashMap<>(4);

		DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			attribute(URI_TEMPLATE_ATTRIBUTE, uriTemplate);
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			return uri(uriFunction.apply(uriBuilderFactory.builder()));
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.uri = uri;
			return this;
		}

		private HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
			}
			return this.headers;
		}

		private MultiValueMap<String, String> getCookies() {
			if (this.cookies == null) {
				this.cookies = new LinkedMultiValueMap<>(4);
			}
			return this.cookies;
		}

		@Override
		public DefaultRequestBodyUriSpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(getHeaders());
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			this.attributes.put(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			attributesConsumer.accept(this.attributes);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec accept(MediaType... acceptableMediaTypes) {
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec acceptCharset(Charset... acceptableCharsets) {
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec contentType(MediaType contentType) {
			getHeaders().setContentType(contentType);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec contentLength(long contentLength) {
			getHeaders().setContentLength(contentLength);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec cookie(String name, String value) {
			getCookies().add(name, value);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			cookiesConsumer.accept(getCookies());
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			getHeaders().setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec ifNoneMatch(String... ifNoneMatches) {
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
			this.inserter = inserter;
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(
				P publisher, ParameterizedTypeReference<T> typeReference) {

			this.inserter = BodyInserters.fromPublisher(publisher, typeReference);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> syncBody(Object body) {
			Assert.isTrue(!(body instanceof Publisher),
					"Please specify the element class by using body(Publisher, Class)");
			this.inserter = BodyInserters.fromObject(body);
			return this;
		}

		@Override
		public Mono<ClientResponse> exchange() {
			ClientRequest request = (this.inserter != null ?
					initRequestBuilder().body(this.inserter).build() :
					initRequestBuilder().build());
			return exchangeFunction.exchange(request).switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR);
		}

		private ClientRequest.Builder initRequestBuilder() {
			if (defaultRequest != null) {
				defaultRequest.accept(this);
			}
			return ClientRequest.create(this.httpMethod, initUri())
					.headers(headers -> headers.addAll(initHeaders()))
					.cookies(cookies -> cookies.addAll(initCookies()))
					.attributes(attributes -> attributes.putAll(this.attributes));
		}

		private URI initUri() {
			return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
		}

		private HttpHeaders initHeaders() {
			if (CollectionUtils.isEmpty(this.headers)) {
				return (defaultHeaders != null ? defaultHeaders : new HttpHeaders());
			}
			else if (CollectionUtils.isEmpty(defaultHeaders)) {
				return this.headers;
			}
			else {
				HttpHeaders result = new HttpHeaders();
				result.putAll(defaultHeaders);
				result.putAll(this.headers);
				return result;
			}
		}

		private MultiValueMap<String, String> initCookies() {
			if (CollectionUtils.isEmpty(this.cookies)) {
				return (defaultCookies != null ? defaultCookies : new LinkedMultiValueMap<>());
			}
			else if (CollectionUtils.isEmpty(defaultCookies)) {
				return this.cookies;
			}
			else {
				MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
				result.putAll(defaultCookies);
				result.putAll(this.cookies);
				return result;
			}
		}

		@Override
		public ResponseSpec retrieve() {
			return new DefaultResponseSpec(exchange(), this::createRequest);
		}

		private HttpRequest createRequest() {
			return new HttpRequest() {
				private final URI uri = initUri();
				private final HttpHeaders headers = initHeaders();

				@Override
				public HttpMethod getMethod() {
					return httpMethod;
				}
				@Override
				public String getMethodValue() {
					return httpMethod.name();
				}
				@Override
				public URI getURI() {
					return this.uri;
				}
				@Override
				public HttpHeaders getHeaders() {
					return this.headers;
				}
			};
		}
	}


	private static class DefaultResponseSpec implements ResponseSpec {

		private static final StatusHandler DEFAULT_STATUS_HANDLER =
				new StatusHandler(HttpStatus::isError, DefaultResponseSpec::createResponseException);

		private final Mono<ClientResponse> responseMono;

		private final Supplier<HttpRequest> requestSupplier;

		private final List<StatusHandler> statusHandlers = new ArrayList<>(1);

		DefaultResponseSpec(Mono<ClientResponse> responseMono, Supplier<HttpRequest> requestSupplier) {
			this.responseMono = responseMono;
			this.requestSupplier = requestSupplier;
			this.statusHandlers.add(DEFAULT_STATUS_HANDLER);
		}

		@Override
		public ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

			if (this.statusHandlers.size() == 1 && this.statusHandlers.get(0) == DEFAULT_STATUS_HANDLER) {
				this.statusHandlers.clear();
			}
			this.statusHandlers.add(new StatusHandler(statusPredicate,
					(clientResponse, request) -> exceptionFunction.apply(clientResponse)));

			return this;
		}

		@Override
		public <T> Mono<T> bodyToMono(Class<T> bodyType) {
			return this.responseMono.flatMap(response -> handleBody(response,
					response.bodyToMono(bodyType), mono -> mono.flatMap(Mono::error)));
		}

		@Override
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> bodyType) {
			return this.responseMono.flatMap(response ->
					handleBody(response, response.bodyToMono(bodyType), mono -> mono.flatMap(Mono::error)));
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<T> elementType) {
			return this.responseMono.flatMapMany(response ->
					handleBody(response, response.bodyToFlux(elementType), mono -> mono.flatMapMany(Flux::error)));
		}

		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementType) {
			return this.responseMono.flatMapMany(response -> handleBody(response,
					response.bodyToFlux(elementType), mono -> mono.flatMapMany(Flux::error)));
		}

		private <T extends Publisher<?>> T handleBody(ClientResponse response,
				T bodyPublisher, Function<Mono<? extends Throwable>, T> errorFunction) {

			if (HttpStatus.resolve(response.rawStatusCode()) != null) {
				for (StatusHandler handler : this.statusHandlers) {
					if (handler.test(response.statusCode())) {
						HttpRequest request = this.requestSupplier.get();
						Mono<? extends Throwable> exMono;
						try {
							exMono = handler.apply(response, request);
							exMono = exMono.flatMap(ex -> drainBody(response, ex));
							exMono = exMono.onErrorResume(ex -> drainBody(response, ex));
						}
						catch (Throwable ex2) {
							exMono = drainBody(response, ex2);
						}
						return errorFunction.apply(exMono);
					}
				}
				return bodyPublisher;
			}
			else {
				return errorFunction.apply(createResponseException(response, this.requestSupplier.get()));
			}
		}

		@SuppressWarnings("unchecked")
		private <T> Mono<T> drainBody(ClientResponse response, Throwable ex) {
			// Ensure the body is drained, even if the StatusHandler didn't consume it,
			// but ignore exception, in case the handler did consume.
			return (Mono<T>) response.bodyToMono(Void.class)
					.onErrorResume(ex2 -> Mono.empty()).thenReturn(ex);
		}

		private static Mono<WebClientResponseException> createResponseException(
				ClientResponse response, HttpRequest request) {

			return DataBufferUtils.join(response.body(BodyExtractors.toDataBuffers()))
					.map(dataBuffer -> {
						byte[] bytes = new byte[dataBuffer.readableByteCount()];
						dataBuffer.read(bytes);
						DataBufferUtils.release(dataBuffer);
						return bytes;
					})
					.defaultIfEmpty(new byte[0])
					.map(bodyBytes -> {
						Charset charset = response.headers().contentType()
								.map(MimeType::getCharset)
								.orElse(StandardCharsets.ISO_8859_1);
						if (HttpStatus.resolve(response.rawStatusCode()) != null) {
							return WebClientResponseException.create(
									response.statusCode().value(),
									response.statusCode().getReasonPhrase(),
									response.headers().asHttpHeaders(),
									bodyBytes,
									charset,
									request);
						}
						else {
							return new UnknownHttpStatusCodeException(
									response.rawStatusCode(),
									response.headers().asHttpHeaders(),
									bodyBytes,
									charset,
									request);
						}
					});
		}


		private static class StatusHandler {

			private final Predicate<HttpStatus> predicate;

			private final BiFunction<ClientResponse, HttpRequest, Mono<? extends Throwable>> exceptionFunction;

			public StatusHandler(Predicate<HttpStatus> predicate,
					BiFunction<ClientResponse, HttpRequest, Mono<? extends Throwable>> exceptionFunction) {

				Assert.notNull(predicate, "Predicate must not be null");
				Assert.notNull(exceptionFunction, "Function must not be null");
				this.predicate = predicate;
				this.exceptionFunction = exceptionFunction;
			}

			public boolean test(HttpStatus status) {
				return this.predicate.test(status);
			}

			public Mono<? extends Throwable> apply(ClientResponse response, HttpRequest request) {
				return this.exceptionFunction.apply(response, request);
			}
		}
	}

}
