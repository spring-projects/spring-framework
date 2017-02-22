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
import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Package-private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

	private final ServerHttpRequest delegate;

	private HttpMethod httpMethod;

	private String path;

	private String contextPath;

	private HttpHeaders httpHeaders;


	public DefaultServerHttpRequestBuilder(ServerHttpRequest delegate) {
		Assert.notNull(delegate, "ServerHttpRequest delegate is required");
		this.delegate = delegate;
	}


	@Override
	public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder path(String path) {
		this.path = path;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder header(String key, String value) {
		if (this.httpHeaders == null) {
			this.httpHeaders = new HttpHeaders();
		}
		this.httpHeaders.add(key, value);
		return this;
	}

	@Override
	public ServerHttpRequest build() {
		URI uri = null;
		if (this.path != null) {
			uri = this.delegate.getURI();
			try {
				uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
						this.path, uri.getQuery(), uri.getFragment());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Invalid URI path: \"" + this.path + "\"");
			}
		}
		return new MutativeDecorator(this.delegate, this.httpMethod, uri, this.contextPath, this.httpHeaders);
	}


	/**
	 * An immutable wrapper of a request returning property overrides -- given
	 * to the constructor -- or original values otherwise.
	 */
	private static class MutativeDecorator extends ServerHttpRequestDecorator {

		private final HttpMethod httpMethod;

		private final URI uri;

		private final String contextPath;

		private final HttpHeaders httpHeaders;

		public MutativeDecorator(ServerHttpRequest delegate, HttpMethod httpMethod,
				URI uri, String contextPath, HttpHeaders httpHeaders) {

			super(delegate);
			this.httpMethod = httpMethod;
			this.uri = uri;
			this.contextPath = contextPath;
			if (httpHeaders != null) {
				this.httpHeaders = new HttpHeaders();
				this.httpHeaders.putAll(super.getHeaders());
				this.httpHeaders.putAll(httpHeaders);
			}
			else {
				this.httpHeaders = null;
			}
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
		public HttpHeaders getHeaders() {
			return (this.httpHeaders != null ? this.httpHeaders : super.getHeaders());
		}
	}

}
