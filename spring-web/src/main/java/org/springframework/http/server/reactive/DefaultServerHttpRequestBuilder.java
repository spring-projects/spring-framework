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

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Package private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

	private final ServerHttpRequest delegate;

	private HttpMethod httpMethod;

	private String path;

	private String contextPath;


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
	public ServerHttpRequest build() {
		URI uri = null;
		if (this.path != null) {
			uri = this.delegate.getURI();
			uri = UriComponentsBuilder.fromUri(uri).replacePath(this.path).build(true).toUri();
		}
		return new MutativeDecorator(this.delegate, this.httpMethod, uri, this.contextPath);
	}


	/**
	 * An immutable wrapper of a request returning property overrides -- given
	 * to the constructor -- or original values otherwise.
	 */
	private static class MutativeDecorator extends ServerHttpRequestDecorator {

		private final HttpMethod httpMethod;

		private final URI uri;

		private final String contextPath;


		public MutativeDecorator(ServerHttpRequest delegate, HttpMethod httpMethod,
				URI uri, String contextPath) {

			super(delegate);
			this.httpMethod = httpMethod;
			this.uri = uri;
			this.contextPath = contextPath;
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

	}

}
