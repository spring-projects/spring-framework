/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * {@code Request} implementation based on a {@link ServerWebExchange}.
 * @author Arjen Poutsma
 */
class DefaultRequest implements Request {

	private final ServerWebExchange exchange;

	private final Headers headers;

	private final Body body;

	DefaultRequest(ServerWebExchange exchange) {
		this.exchange = exchange;
		this.headers = new DefaultHeaders();
		this.body = new DefaultBody();
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
	public Body body() {
		return this.body;
	}

	@Override
	public <T> Optional<T> attribute(String name) {
		return this.exchange.getAttribute(name);
	}

	@Override
	public List<String> queryParams(String name) {
		List<String> queryParams = request().getQueryParams().get(name);
		return queryParams != null ? queryParams : Collections.emptyList();
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.exchange.<Map<String, String>>getAttribute(Router.URI_TEMPLATE_VARIABLES_ATTRIBUTE).
				orElseGet(Collections::emptyMap);
	}

	private ServerHttpRequest request() {
		return this.exchange.getRequest();
	}

	ServerWebExchange exchange() {
		return this.exchange;
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
		public OptionalLong contentLength() {
			return toOptionalLong(delegate().getContentLength());
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
			return headerValues != null ? headerValues : Collections.emptyList();
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(delegate());
		}

		private OptionalLong toOptionalLong(long value) {
			return value != -1 ? OptionalLong.of(value) : OptionalLong.empty();
		}

	}

	private class DefaultBody implements Body {

		@Override
		public Flux<DataBuffer> stream() {
			return request().getBody();
		}

		@Override
		public <T> Flux<T> convertTo(Class<? extends T> aClass) {
			ResolvableType elementType = ResolvableType.forClass(aClass);
			return convertTo(aClass, reader -> reader.read(elementType, request()));
		}

		@Override
		public <T> Mono<T> convertToMono(Class<? extends T> aClass) {
			ResolvableType elementType = ResolvableType.forClass(aClass);
			return convertTo(aClass, reader -> reader.readMono(elementType, request()));
		}

		private <T, S extends Publisher<T>> S convertTo(Class<? extends T> targetClass,
				Function<HttpMessageReader<T>, S> readerFunction) {
			ResolvableType elementType = ResolvableType.forClass(targetClass);
			MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
			return messageReaderStream(exchange)
					.filter(r -> r.canRead(elementType, contentType))
					.findFirst()
					.map(CastingUtils::<T>cast)
					.map(readerFunction)
					.orElseGet(() -> {
						List<MediaType> supportedMediaTypes = messageReaderStream(exchange)
								.flatMap(messageReader -> messageReader.getReadableMediaTypes().stream())
								.collect(Collectors.toList());
						return cast(
								Mono.<T>error(new UnsupportedMediaTypeStatusException(contentType, supportedMediaTypes)));
					});
		}

		private Stream<HttpMessageReader<?>> messageReaderStream(ServerWebExchange exchange) {
			return exchange.<Stream<HttpMessageReader<?>>>getAttribute(Router.HTTP_MESSAGE_READERS_ATTRIBUTE)
					.orElseThrow(() -> new IllegalStateException("Could not find HttpMessageReaders in ServerWebExchange"));
		}

		@SuppressWarnings("unchecked")
		private <T, S extends Publisher<T>> S cast(Mono<T> mono) {
			return (S) mono;
		}

	}

}
