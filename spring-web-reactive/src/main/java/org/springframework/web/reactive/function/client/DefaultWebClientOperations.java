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
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;


/**
 * Default implementation of {@link WebClientOperations}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientOperations implements WebClientOperations {

	private final WebClient webClient;

	private final UriBuilderFactory uriBuilderFactory;


	DefaultWebClientOperations(WebClient webClient, UriBuilderFactory factory) {
		this.webClient = webClient;
		this.uriBuilderFactory = (factory != null ? factory : new DefaultUriBuilderFactory());
	}


	private WebClient getWebClient() {
		return this.webClient;
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
	public WebClientOperations filter(ExchangeFilterFunction filterFunction) {
		WebClient filteredWebClient = this.webClient.filter(filterFunction);
		return new DefaultWebClientOperations(filteredWebClient, this.uriBuilderFactory);
	}


	private class DefaultUriSpec implements UriSpec {

		private final HttpMethod httpMethod;


		DefaultUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public HeaderSpec uri(URI uri) {
			return new DefaultHeaderSpec(ClientRequest.method(this.httpMethod, uri));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Object... uriVariables) {
			return uri(getUriBuilderFactory().expand(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(Function<UriBuilderFactory, URI> uriFunction) {
			return uri(uriFunction.apply(getUriBuilderFactory()));
		}
	}

	private class DefaultHeaderSpec implements HeaderSpec {

		private final ClientRequest.Builder requestBuilder;

		private final HttpHeaders headers = new HttpHeaders();


		DefaultHeaderSpec(ClientRequest.Builder requestBuilder) {
			this.requestBuilder = requestBuilder;
		}


		@Override
		public DefaultHeaderSpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public DefaultHeaderSpec headers(HttpHeaders headers) {
			if (headers != null) {
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public DefaultHeaderSpec accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public DefaultHeaderSpec acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public DefaultHeaderSpec contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookie(String name, String value) {
			this.requestBuilder.cookie(name, value);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookies(MultiValueMap<String, String> cookies) {
			this.requestBuilder.cookies(cookies);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			ZonedDateTime gmt = ifModifiedSince.withZoneSameInstant(ZoneId.of("GMT"));
			String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
			this.headers.set(HttpHeaders.IF_MODIFIED_SINCE, headerValue);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public Mono<ClientResponse> exchange() {
			ClientRequest<Void> request = this.requestBuilder.headers(this.headers).build();
			return getWebClient().exchange(request);
		}

		@Override
		public <T> Mono<ClientResponse> exchange(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			ClientRequest<T> request = this.requestBuilder.headers(this.headers).body(inserter);
			return getWebClient().exchange(request);
		}

		@Override
		public <T, S extends Publisher<T>> Mono<ClientResponse> exchange(S publisher, Class<T> elementClass) {
			ClientRequest<S> request = this.requestBuilder.headers(this.headers).body(publisher, elementClass);
			return getWebClient().exchange(request);
		}
	}

}
