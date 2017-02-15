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

package org.springframework.web.reactive.function.server.support;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.WebSession;

/**
 * Implementation of the {@link ServerRequest} interface that can be subclassed
 * to adapt the request to a {@link HandlerFunction handler function}.
 * All methods default to calling through to the wrapped request.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerRequestWrapper implements ServerRequest {

	private final ServerRequest delegate;


	/**
	 * Create a new {@code RequestWrapper} that wraps the given request.
	 * @param delegate the request to wrap
	 */
	public ServerRequestWrapper(ServerRequest delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped request.
	 */
	public ServerRequest request() {
		return this.delegate;
	}

	@Override
	public HttpMethod method() {
		return this.delegate.method();
	}

	@Override
	public URI uri() {
		return this.delegate.uri();
	}

	@Override
	public String path() {
		return this.delegate.path();
	}

	@Override
	public Headers headers() {
		return this.delegate.headers();
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
		return this.delegate.body(extractor);
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
		return this.delegate.body(extractor, hints);
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return this.delegate.bodyToMono(elementClass);
	}

	@Override
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return this.delegate.bodyToFlux(elementClass);
	}

	@Override
	public <T> Optional<T> attribute(String name) {
		return this.delegate.attribute(name);
	}

	@Override
	public Optional<String> queryParam(String name) {
		return this.delegate.queryParam(name);
	}

	@Override
	public List<String> queryParams(String name) {
		return this.delegate.queryParams(name);
	}

	@Override
	public String pathVariable(String name) {
		return this.delegate.pathVariable(name);
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.delegate.pathVariables();
	}

	@Override
	public Mono<WebSession> session() {
		return this.delegate.session();
	}


	/**
	 * Implementation of the {@code Headers} interface that can be subclassed
	 * to adapt the headers to a {@link HandlerFunction handler function}.
	 * All methods default to calling through to the wrapped headers.
	 */
	public static class HeadersWrapper implements ServerRequest.Headers {

		private final Headers headers;

		/**
		 * Create a new {@code HeadersWrapper} that wraps the given request.
		 * @param headers the headers to wrap
		 */
		public HeadersWrapper(Headers headers) {
			Assert.notNull(headers, "'headers' must not be null");
			this.headers = headers;
		}

		@Override
		public List<MediaType> accept() {
			return this.headers.accept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return this.headers.acceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return this.headers.acceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			return this.headers.contentLength();
		}

		@Override
		public Optional<MediaType> contentType() {
			return this.headers.contentType();
		}

		@Override
		public InetSocketAddress host() {
			return this.headers.host();
		}

		@Override
		public List<HttpRange> range() {
			return this.headers.range();
		}

		@Override
		public List<String> header(String headerName) {
			return this.headers.header(headerName);
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.headers.asHttpHeaders();
		}
	}

}
