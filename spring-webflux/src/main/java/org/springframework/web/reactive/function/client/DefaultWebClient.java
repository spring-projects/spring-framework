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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

	private final ExchangeFunction exchangeFunction;

	private final UriBuilderFactory uriBuilderFactory;

	private final HttpHeaders defaultHeaders;

	private final MultiValueMap<String, String> defaultCookies;


	DefaultWebClient(ExchangeFunction exchangeFunction, UriBuilderFactory factory,
			HttpHeaders defaultHeaders, MultiValueMap<String, String> defaultCookies) {

		this.exchangeFunction = exchangeFunction;
		this.uriBuilderFactory = (factory != null ? factory : new DefaultUriBuilderFactory());
		this.defaultHeaders = (defaultHeaders != null ?
				HttpHeaders.readOnlyHttpHeaders(defaultHeaders) : null);
		this.defaultCookies = (defaultCookies != null ?
				CollectionUtils.unmodifiableMultiValueMap(defaultCookies) : null);
	}


	@Override
	public UriSpec<RequestHeadersSpec<?>> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> head() {
		return methodInternal(HttpMethod.HEAD);
	}

	@Override
	public UriSpec<RequestBodySpec> post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public UriSpec<RequestBodySpec> put() {
		return methodInternal(HttpMethod.PUT);
	}

	@Override
	public UriSpec<RequestBodySpec> patch() {
		return methodInternal(HttpMethod.PATCH);
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> delete() {
		return methodInternal(HttpMethod.DELETE);
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> options() {
		return methodInternal(HttpMethod.OPTIONS);
	}

	@Override
	public UriSpec<RequestBodySpec> method(HttpMethod httpMethod) {
		return methodInternal(httpMethod);
	}

	@SuppressWarnings("unchecked")
	private <S extends RequestHeadersSpec<?>> UriSpec<S> methodInternal(HttpMethod httpMethod) {
		return new DefaultUriSpec<>(httpMethod);
	}

	@Override
	public WebClient filter(ExchangeFilterFunction filterFunction) {
		ExchangeFunction filteredExchangeFunction = this.exchangeFunction.filter(filterFunction);
		return new DefaultWebClient(filteredExchangeFunction,
				this.uriBuilderFactory, this.defaultHeaders, this.defaultCookies);
	}


	private class DefaultUriSpec<S extends RequestHeadersSpec<?>> implements UriSpec<S> {

		private final HttpMethod httpMethod;


		DefaultUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public S uri(String uriTemplate, Object... uriVariables) {
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public S uri(String uriTemplate, Map<String, ?> uriVariables) {
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public S uri(Function<UriBuilder, URI> uriFunction) {
			return uri(uriFunction.apply(uriBuilderFactory.builder()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public S uri(URI uri) {
			return (S) new DefaultRequestBodySpec(this.httpMethod, uri);
		}
	}


	private class DefaultRequestBodySpec implements RequestBodySpec {

		private final HttpMethod httpMethod;

		private final URI uri;

		private HttpHeaders headers;

		private MultiValueMap<String, String> cookies;

		private BodyInserter<?, ? super ClientHttpRequest> inserter;

		DefaultRequestBodySpec(HttpMethod httpMethod, URI uri) {
			this.httpMethod = httpMethod;
			this.uri = uri;
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
		public DefaultRequestBodySpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public DefaultRequestBodySpec headers(HttpHeaders headers) {
			if (headers != null) {
				getHeaders().putAll(headers);
			}
			return this;
		}

		@Override
		public DefaultRequestBodySpec accept(MediaType... acceptableMediaTypes) {
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public DefaultRequestBodySpec acceptCharset(Charset... acceptableCharsets) {
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public DefaultRequestBodySpec contentType(MediaType contentType) {
			getHeaders().setContentType(contentType);
			return this;
		}

		@Override
		public DefaultRequestBodySpec contentLength(long contentLength) {
			getHeaders().setContentLength(contentLength);
			return this;
		}

		@Override
		public DefaultRequestBodySpec cookie(String name, String value) {
			getCookies().add(name, value);
			return this;
		}

		@Override
		public DefaultRequestBodySpec cookies(MultiValueMap<String, String> cookies) {
			if (cookies != null) {
				getCookies().putAll(cookies);
			}
			return this;
		}

		@Override
		public DefaultRequestBodySpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			ZonedDateTime gmt = ifModifiedSince.withZoneSameInstant(ZoneId.of("GMT"));
			String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
			getHeaders().set(HttpHeaders.IF_MODIFIED_SINCE, headerValue);
			return this;
		}

		@Override
		public DefaultRequestBodySpec ifNoneMatch(String... ifNoneMatches) {
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public <T> RequestHeadersSpec<?> body(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			this.inserter = inserter;
			return this;
		}

		@Override
		public <T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
			return this;
		}

		@Override
		public <T> RequestHeadersSpec<?> body(T body) {
			this.inserter = BodyInserters.fromObject(body);
			return this;
		}

		@Override
		public Mono<ClientResponse> exchange() {

			ClientRequest request = this.inserter != null ?
					initRequestBuilder().body(this.inserter).build() :
					initRequestBuilder().build();

			return exchangeFunction.exchange(request);
		}

		private ClientRequest.Builder initRequestBuilder() {
			return ClientRequest.method(this.httpMethod, this.uri).headers(initHeaders()).cookies(initCookies());
		}

		private HttpHeaders initHeaders() {
			if (CollectionUtils.isEmpty(defaultHeaders) && CollectionUtils.isEmpty(this.headers)) {
				return null;
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
				return null;
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
	}

}
