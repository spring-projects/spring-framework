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

import org.jetbrains.annotations.NotNull;
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
import org.springframework.web.util.DefaultUriBuilderFactory;
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

		this.defaultHeaders = defaultHeaders != null ?
				HttpHeaders.readOnlyHttpHeaders(defaultHeaders) : null;

		this.defaultCookies = defaultCookies != null ?
				CollectionUtils.unmodifiableMultiValueMap(defaultCookies) : null;
	}


	private ExchangeFunction getExchangeFunction() {
		return this.exchangeFunction;
	}

	private UriBuilderFactory getUriBuilderFactory() {
		return this.uriBuilderFactory;
	}


	@Override
	public UriSpec get() {
		return method(HttpMethod.GET);
	}

	@Override
	public UriSpec head() {
		return method(HttpMethod.HEAD);
	}

	@Override
	public UriSpec post() {
		return method(HttpMethod.POST);
	}

	@Override
	public UriSpec put() {
		return method(HttpMethod.PUT);
	}

	@Override
	public UriSpec patch() {
		return method(HttpMethod.PATCH);
	}

	@Override
	public UriSpec delete() {
		return method(HttpMethod.DELETE);
	}

	@Override
	public UriSpec options() {
		return method(HttpMethod.OPTIONS);
	}

	@NotNull
	private UriSpec method(HttpMethod httpMethod) {
		return new DefaultUriSpec(httpMethod);
	}


	@Override
	public WebClient filter(ExchangeFilterFunction filterFunction) {
		ExchangeFunction filteredExchangeFunction = this.exchangeFunction.filter(filterFunction);
		return new DefaultWebClient(filteredExchangeFunction,
				this.uriBuilderFactory, this.defaultHeaders, this.defaultCookies);
	}


	private class DefaultUriSpec implements UriSpec {

		private final HttpMethod httpMethod;


		DefaultUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Object... uriVariables) {
			return uri(getUriBuilderFactory().expand(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			return uri(getUriBuilderFactory().expand(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(Function<UriBuilderFactory, URI> uriFunction) {
			return uri(uriFunction.apply(getUriBuilderFactory()));
		}

		@Override
		public HeaderSpec uri(URI uri) {
			return new DefaultHeaderSpec(this.httpMethod, uri);
		}
	}

	private class DefaultHeaderSpec implements HeaderSpec {

		private final HttpMethod httpMethod;

		private final URI uri;

		private HttpHeaders headers;

		private MultiValueMap<String, String> cookies;


		DefaultHeaderSpec(HttpMethod httpMethod, URI uri) {
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
		public DefaultHeaderSpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public DefaultHeaderSpec headers(HttpHeaders headers) {
			if (headers != null) {
				getHeaders().putAll(headers);
			}
			return this;
		}

		@Override
		public DefaultHeaderSpec accept(MediaType... acceptableMediaTypes) {
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public DefaultHeaderSpec acceptCharset(Charset... acceptableCharsets) {
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public DefaultHeaderSpec contentType(MediaType contentType) {
			getHeaders().setContentType(contentType);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentLength(long contentLength) {
			getHeaders().setContentLength(contentLength);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookie(String name, String value) {
			getCookies().add(name, value);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookies(MultiValueMap<String, String> cookies) {
			if (cookies != null) {
				getCookies().putAll(cookies);
			}
			return this;
		}

		@Override
		public DefaultHeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			ZonedDateTime gmt = ifModifiedSince.withZoneSameInstant(ZoneId.of("GMT"));
			String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
			getHeaders().set(HttpHeaders.IF_MODIFIED_SINCE, headerValue);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifNoneMatch(String... ifNoneMatches) {
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public Mono<ClientResponse> exchange() {
			ClientRequest<Void> request = initRequestBuilder().build();
			return getExchangeFunction().exchange(request);
		}

		@Override
		public <T> Mono<ClientResponse> exchange(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			ClientRequest<T> request = initRequestBuilder().body(inserter);
			return getExchangeFunction().exchange(request);
		}

		@Override
		public <T, S extends Publisher<T>> Mono<ClientResponse> exchange(S publisher, Class<T> elementClass) {
			ClientRequest<S> request = initRequestBuilder().headers(this.headers).body(publisher, elementClass);
			return getExchangeFunction().exchange(request);
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
