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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
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
 * @since 5.0
 */
class DefaultWebClient implements WebClient {

	private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
			new IllegalStateException("The underlying HTTP client completed without emitting a response."));


	private final ExchangeFunction exchangeFunction;

	private final UriBuilderFactory uriBuilderFactory;

	@Nullable
	private final HttpHeaders defaultHeaders;

	@Nullable
	private final MultiValueMap<String, String> defaultCookies;

	private final DefaultWebClientBuilder builder;


	DefaultWebClient(ExchangeFunction exchangeFunction, @Nullable UriBuilderFactory factory,
			@Nullable HttpHeaders defaultHeaders, @Nullable MultiValueMap<String, String> defaultCookies,
			DefaultWebClientBuilder builder) {

		this.exchangeFunction = exchangeFunction;
		this.uriBuilderFactory = (factory != null ? factory : new DefaultUriBuilderFactory());
		this.defaultHeaders = defaultHeaders;
		this.defaultCookies = defaultCookies;
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

	@SuppressWarnings("unchecked")
	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(httpMethod);
	}

	@Override
	public Builder mutate() {
		return this.builder;
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

		@Nullable
		private Map<String, Object> attributes;

		DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
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

		private Map<String, Object> getAttributes() {
			if (this.attributes == null) {
				this.attributes = new LinkedHashMap<>(4);
			}
			return this.attributes;
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
			Assert.notNull(headersConsumer, "'headersConsumer' must not be null");
			headersConsumer.accept(getHeaders());
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			getAttributes().put(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			Assert.notNull(attributesConsumer, "'attributesConsumer' must not be null");
			attributesConsumer.accept(getAttributes());
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
		public DefaultRequestBodyUriSpec cookies(
				Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			Assert.notNull(cookiesConsumer, "'cookiesConsumer' must not be null");
			cookiesConsumer.accept(this.cookies);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			ZonedDateTime gmt = ifModifiedSince.withZoneSameInstant(ZoneId.of("GMT"));
			String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
			getHeaders().set(HttpHeaders.IF_MODIFIED_SINCE, headerValue);
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
			URI uri = this.uri != null ? this.uri : uriBuilderFactory.expand("");
			return ClientRequest.method(this.httpMethod, uri)
					.headers(headers -> headers.addAll(initHeaders()))
					.cookies(cookies -> cookies.addAll(initCookies()))
					.attributes(attributes -> attributes.putAll(getAttributes()));
		}

		private HttpHeaders initHeaders() {
			if (CollectionUtils.isEmpty(defaultHeaders) && CollectionUtils.isEmpty(this.headers)) {
				return new HttpHeaders();
			}
			else if (CollectionUtils.isEmpty(defaultHeaders)) {
				return this.headers;
			}
			else if (CollectionUtils.isEmpty(this.headers)) {
				return defaultHeaders;
			}
			else {
				HttpHeaders result = new HttpHeaders();
				result.putAll(this.headers);
				defaultHeaders.forEach((name, values) -> {
					if (!this.headers.containsKey(name)) {
						values.forEach(value -> result.add(name, value));
					}
				});
				return result;
			}
		}

		private MultiValueMap<String, String> initCookies() {
			if (CollectionUtils.isEmpty(defaultCookies) && CollectionUtils.isEmpty(this.cookies)) {
				return new LinkedMultiValueMap<>(0);
			}
			else if (CollectionUtils.isEmpty(defaultCookies)) {
				return this.cookies;
			}
			else if (CollectionUtils.isEmpty(this.cookies)) {
				return defaultCookies;
			}
			else {
				MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
				result.putAll(this.cookies);
				defaultCookies.forEach(result::putIfAbsent);
				return result;
			}
		}

		@Override
		public ResponseSpec retrieve() {
			return new DefaultResponseSpec(exchange());
		}
	}

	private static class DefaultResponseSpec implements ResponseSpec {

		private static final Function<ClientResponse, Optional<? extends Throwable>> DEFAULT_STATUS_HANDLER =
				clientResponse -> {
					HttpStatus statusCode = clientResponse.statusCode();
					if (statusCode.isError()) {
						return Optional.of(new WebClientException(
								"ClientResponse has erroneous status code: " + statusCode.value() +
										" " + statusCode.getReasonPhrase()));
					} else {
						return Optional.empty();
					}
				};

		private final Mono<ClientResponse> responseMono;

		private List<Function<ClientResponse, Optional<? extends Throwable>>> statusHandlers =
				new ArrayList<>(1);


		DefaultResponseSpec(Mono<ClientResponse> responseMono) {
			this.responseMono = responseMono;
			this.statusHandlers.add(DEFAULT_STATUS_HANDLER);
		}

		@Override
		public ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
				Function<ClientResponse, ? extends Throwable> exceptionFunction) {

			Assert.notNull(statusPredicate, "'statusPredicate' must not be null");
			Assert.notNull(exceptionFunction, "'exceptionFunction' must not be null");

			if (this.statusHandlers.size() == 1 && this.statusHandlers.get(0) == DEFAULT_STATUS_HANDLER) {
				this.statusHandlers.clear();
			}

			Function<ClientResponse, Optional<? extends Throwable>> statusHandler =
					clientResponse -> {
						if (statusPredicate.test(clientResponse.statusCode())) {
							return Optional.of(exceptionFunction.apply(clientResponse));
						}
						else {
							return Optional.empty();
						}
					};
			this.statusHandlers.add(statusHandler);

			return this;
		}

		@Override
		public <T> Mono<T> bodyToMono(Class<T> bodyType) {
			return this.responseMono.flatMap(
					response -> bodyToPublisher(response, BodyExtractors.toMono(bodyType),
							Mono::error));
		}

		@Override
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
			return this.responseMono.flatMap(
					response -> bodyToPublisher(response, BodyExtractors.toMono(typeReference),
							Mono::error));
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<T> elementType) {
			return this.responseMono.flatMapMany(
					response -> bodyToPublisher(response, BodyExtractors.toFlux(elementType),
							Flux::error));
		}

		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
			return this.responseMono.flatMapMany(
					response -> bodyToPublisher(response, BodyExtractors.toFlux(typeReference),
							Flux::error));
		}

		private <T extends Publisher<?>> T bodyToPublisher(ClientResponse response,
				BodyExtractor<T, ? super ClientHttpResponse> extractor,
				Function<Throwable, T> errorFunction) {

			return this.statusHandlers.stream()
					.map(statusHandler -> statusHandler.apply(response))
					.filter(Optional::isPresent)
					.findFirst()
					.map(Optional::get)
					.map(errorFunction::apply)
					.orElse(response.body(extractor));
		}

	}
}
