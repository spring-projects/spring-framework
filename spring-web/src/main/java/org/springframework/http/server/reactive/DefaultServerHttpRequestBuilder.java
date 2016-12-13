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
package org.springframework.http.server.reactive;

import java.net.URI;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Package private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

	private final ServerHttpRequest delegate;


	private HttpMethod httpMethod;

	private URI uri;

	private String contextPath;

	private MultiValueMap<String, String> queryParams;

	private HttpHeaders headers;

	private MultiValueMap<String, HttpCookie> cookies;

	private Flux<DataBuffer> body;


	public DefaultServerHttpRequestBuilder(ServerHttpRequest delegate) {
		Assert.notNull(delegate, "ServerHttpRequest delegate is required.");
		this.delegate = delegate;
	}


	@Override
	public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder uri(URI uri) {
		this.uri = uri;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder queryParams(MultiValueMap<String, String> queryParams) {
		this.queryParams = queryParams;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder headers(HttpHeaders headers) {
		this.headers = headers;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder cookies(MultiValueMap<String, HttpCookie> cookies) {
		this.cookies = cookies;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder body(Flux<DataBuffer> body) {
		this.body = body;
		return this;
	}

	@Override
	public ServerHttpRequest build() {
		return new MutativeDecorator(this.delegate, this.httpMethod, this.uri, this.contextPath,
				this.queryParams, this.headers, this.cookies, this.body);
	}


	/**
	 * An immutable wrapper of a request returning property overrides -- given
	 * to the constructor -- or original values otherwise.
	 */
	private static class MutativeDecorator extends ServerHttpRequestDecorator {

		private final HttpMethod httpMethod;

		private final URI uri;

		private final String contextPath;

		private final MultiValueMap<String, String> queryParams;

		private final HttpHeaders headers;

		private final MultiValueMap<String, HttpCookie> cookies;

		private final Flux<DataBuffer> body;


		public MutativeDecorator(ServerHttpRequest delegate, HttpMethod httpMethod, URI uri,
				String contextPath, MultiValueMap<String, String> queryParams, HttpHeaders headers,
				MultiValueMap<String, HttpCookie> cookies, Flux<DataBuffer> body) {

			super(delegate);
			this.httpMethod = httpMethod;
			this.uri = uri;
			this.contextPath = contextPath;
			this.queryParams = queryParams;
			this.headers = headers;
			this.cookies = cookies;
			this.body = body;
		}

		@Override
		public HttpMethod getMethod() {
			return (this.httpMethod != null ? this.httpMethod : super.getMethod());
		}

		@Override
		public URI getURI() {
			return (this.uri != null ? this.uri : super.getURI());
		}

		@Override
		public String getContextPath() {
			return (this.contextPath != null ? this.contextPath : super.getContextPath());
		}

		@Override
		public MultiValueMap<String, String> getQueryParams() {
			return (this.queryParams != null ? this.queryParams : super.getQueryParams());
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.headers != null ? this.headers : super.getHeaders());
		}

		@Override
		public MultiValueMap<String, HttpCookie> getCookies() {
			return (this.cookies != null ? this.cookies : super.getCookies());
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return (this.body != null ? this.body : super.getBody());
		}
	}

}
