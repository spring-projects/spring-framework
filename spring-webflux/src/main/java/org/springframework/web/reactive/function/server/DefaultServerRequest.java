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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.WebSession;

/**
 * {@code ServerRequest} implementation based on a {@link ServerWebExchange}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultServerRequest implements ServerRequest {

	private static final Function<UnsupportedMediaTypeException, UnsupportedMediaTypeStatusException> ERROR_MAPPER =
			ex -> ex.getContentType()
					.map(contentType -> new UnsupportedMediaTypeStatusException(contentType,
							ex.getSupportedMediaTypes()))
					.orElseGet(() -> new UnsupportedMediaTypeStatusException(ex.getMessage()));

	private final ServerWebExchange exchange;

	private final Headers headers;

	private final HandlerStrategies strategies;


	DefaultServerRequest(ServerWebExchange exchange, HandlerStrategies strategies) {
		this.exchange = exchange;
		this.strategies = strategies;
		this.headers = new DefaultHeaders();
	}


	@Override
	public HttpMethod method() {
		return request().getMethod();
	}

	@Override
	public URI uri() {
		return request().getURI();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
		return body(extractor, Collections.emptyMap());
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
		Assert.notNull(extractor, "'extractor' must not be null");
		return extractor.extract(request(),
				new BodyExtractor.Context() {
					@Override
					public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
						return DefaultServerRequest.this.strategies.messageReaders();
					}
					@Override
					public Map<String, Object> hints() {
						return hints;
					}
				});
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		Mono<T> mono = body(BodyExtractors.toMono(elementClass));
		return mono.mapError(UnsupportedMediaTypeException.class, ERROR_MAPPER);
	}

	@Override
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		Flux<T> flux = body(BodyExtractors.toFlux(elementClass));
		return flux.mapError(UnsupportedMediaTypeException.class, ERROR_MAPPER);
	}

	@Override
	public <T> Optional<T> attribute(String name) {
		return this.exchange.getAttribute(name);
	}

	@Override
	public Map<String, Object> attributes() {
		return this.exchange.getAttributes();
	}

	@Override
	public List<String> queryParams(String name) {
		List<String> queryParams = request().getQueryParams().get(name);
		return queryParams != null ? queryParams : Collections.emptyList();
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.exchange.<Map<String, String>>getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE).
				orElseGet(Collections::emptyMap);
	}

	@Override
	public Mono<WebSession> session() {
		return this.exchange.getSession();
	}

	private ServerHttpRequest request() {
		return this.exchange.getRequest();
	}

	ServerWebExchange exchange() {
		return this.exchange;
	}

	@Override
	public String toString() {
		return String.format("%s %s", method(), path());
	}

	private class DefaultHeaders implements Headers {

		private HttpHeaders delegate() {
			return request().getHeaders();
		}

		@Override
		public List<MediaType> accept() {
			return delegate().getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return delegate().getAcceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return delegate().getAcceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			long value = delegate().getContentLength();
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(delegate().getContentType());
		}

		@Override
		public InetSocketAddress host() {
			return delegate().getHost();
		}

		@Override
		public List<HttpRange> range() {
			return delegate().getRange();
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = delegate().get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(delegate());
		}

		@Override
		public String toString() {
			return delegate().toString();
		}
	}

}
