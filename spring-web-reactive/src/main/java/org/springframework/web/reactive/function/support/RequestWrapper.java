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

package org.springframework.web.reactive.function.support;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.HandlerFunction;
import org.springframework.web.reactive.function.Request;

/**
 * Implementation of the {@link Request} interface that can be subclassed to adapt the request to a
 * {@link HandlerFunction handler function}. All methods default to calling through to the wrapped request.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class RequestWrapper implements Request {

	private final Request request;


	/**
	 * Create a new {@code RequestWrapper} that wraps the given request.
	 *
	 * @param request the request to wrap
	 */
	public RequestWrapper(Request request) {
		Assert.notNull(request, "'request' must not be null");
		this.request = request;
	}

	/**
	 * Return the wrapped request.
	 */
	public Request request() {
		return this.request;
	}

	@Override
	public HttpMethod method() {
		return this.request.method();
	}

	@Override
	public URI uri() {
		return this.request.uri();
	}

	@Override
	public String path() {
		return this.request.path();
	}

	@Override
	public Headers headers() {
		return this.request.headers();
	}

	@Override
	public Body body() {
		return this.request.body();
	}

	@Override
	public <T> Optional<T> attribute(String name) {
		return this.request.attribute(name);
	}

	@Override
	public Map<String, Object> attributes() {
		return this.request.attributes();
	}

	@Override
	public Optional<String> queryParam(String name) {
		return this.request.queryParam(name);
	}

	@Override
	public List<String> queryParams(String name) {
		return this.request.queryParams(name);
	}

	@Override
	public Optional<String> pathVariable(String name) {
		return this.request.pathVariable(name);
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.request.pathVariables();
	}

	/**
	 * Implementation of the {@link Headers} interface that can be subclassed to adapt the headers to a
	 * {@link HandlerFunction handler function}. All methods default to calling through to the wrapped headers.
	 */
	public static class HeadersWrapper implements Request.Headers {

		private final Headers headers;

		/**
		 * Create a new {@code HeadersWrapper} that wraps the given request.
		 *
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

	/**
	 * Implementation of the {@link Body} interface that can be subclassed to adapt the headers to a
	 * {@link HandlerFunction handler function}. All methods default to calling through to the wrapped body.
	 */
	public static class BodyWrapper implements Request.Body {

		private final Body body;

		/**
		 * Create a new {@code DelegatingBody} that wraps the given body.
		 *
		 * @param body the body to wrap
		 */
		public BodyWrapper(Body body) {
			Assert.notNull(body, "'body' must not be null");
			this.body = body;
		}

		@Override
		public Flux<DataBuffer> stream() {
			return this.body.stream();
		}

		@Override
		public <T> Flux<T> convertTo(Class<? extends T> aClass) {
			return this.body.convertTo(aClass);
		}

		@Override
		public <T> Mono<T> convertToMono(Class<? extends T> aClass) {
			return this.body.convertToMono(aClass);
		}

	}
}
