/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Package-private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

	private URI uri;

	private final HttpHeaders headers;

	private HttpMethod httpMethod;

	@Nullable
	private String uriPath;

	@Nullable
	private String contextPath;

	@Nullable
	private SslInfo sslInfo;

	@Nullable
	private InetSocketAddress remoteAddress;

	private final Flux<DataBuffer> body;

	private final ServerHttpRequest originalRequest;


	public DefaultServerHttpRequestBuilder(ServerHttpRequest original) {
		Assert.notNull(original, "ServerHttpRequest is required");

		this.uri = original.getURI();
		this.headers = HttpHeaders.writableHttpHeaders(original.getHeaders());
		this.httpMethod = original.getMethod();
		this.contextPath = original.getPath().contextPath().value();
		this.remoteAddress = original.getRemoteAddress();
		this.body = original.getBody();
		this.originalRequest = original;
	}


	@Override
	public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
		Assert.notNull(httpMethod, "HttpMethod must not be null");
		this.httpMethod = httpMethod;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder uri(URI uri) {
		this.uri = uri;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder path(String path) {
		Assert.isTrue(path.startsWith("/"), () -> "The path does not have a leading slash: " + path);
		this.uriPath = path;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder header(String headerName, String... headerValues) {
		this.headers.put(headerName, Arrays.asList(headerValues));
		return this;
	}

	@Override
	public ServerHttpRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		Assert.notNull(headersConsumer, "'headersConsumer' must not be null");
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerHttpRequest.Builder sslInfo(SslInfo sslInfo) {
		this.sslInfo = sslInfo;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}

	@Override
	public ServerHttpRequest build() {
		return new MutatedServerHttpRequest(getUriToUse(), this.contextPath,
				this.httpMethod, this.sslInfo, this.remoteAddress, this.headers, this.body, this.originalRequest);
	}

	private URI getUriToUse() {
		if (this.uriPath == null) {
			return this.uri;
		}

		StringBuilder uriBuilder = new StringBuilder();
		if (this.uri.getScheme() != null) {
			uriBuilder.append(this.uri.getScheme()).append(':');
		}
		if (this.uri.getRawUserInfo() != null || this.uri.getHost() != null) {
			uriBuilder.append("//");
			if (this.uri.getRawUserInfo() != null) {
				uriBuilder.append(this.uri.getRawUserInfo()).append('@');
			}
			if (this.uri.getHost() != null) {
				uriBuilder.append(this.uri.getHost());
			}
			if (this.uri.getPort() != -1) {
				uriBuilder.append(':').append(this.uri.getPort());
			}
		}
		if (StringUtils.hasLength(this.uriPath)) {
			uriBuilder.append(this.uriPath);
		}
		if (this.uri.getRawQuery() != null) {
			uriBuilder.append('?').append(this.uri.getRawQuery());
		}
		if (this.uri.getRawFragment() != null) {
			uriBuilder.append('#').append(this.uri.getRawFragment());
		}
		try {
			return new URI(uriBuilder.toString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Invalid URI path: \"" + this.uriPath + "\"", ex);
		}
	}


	private static class MutatedServerHttpRequest extends AbstractServerHttpRequest {

		@Nullable
		private final SslInfo sslInfo;

		@Nullable
		private final InetSocketAddress remoteAddress;

		private final Flux<DataBuffer> body;

		private final ServerHttpRequest originalRequest;


		public MutatedServerHttpRequest(URI uri, @Nullable String contextPath,
				HttpMethod method, @Nullable SslInfo sslInfo, @Nullable InetSocketAddress remoteAddress,
				HttpHeaders headers, Flux<DataBuffer> body, ServerHttpRequest originalRequest) {

			super(method, uri, contextPath, headers);
			this.remoteAddress = (remoteAddress != null ? remoteAddress : originalRequest.getRemoteAddress());
			this.sslInfo = (sslInfo != null ? sslInfo : originalRequest.getSslInfo());
			this.body = body;
			this.originalRequest = originalRequest;
		}

		@Override
		protected MultiValueMap<String, HttpCookie> initCookies() {
			return this.originalRequest.getCookies();
		}

		@Override
		@Nullable
		public InetSocketAddress getLocalAddress() {
			return this.originalRequest.getLocalAddress();
		}

		@Override
		@Nullable
		public InetSocketAddress getRemoteAddress() {
			return this.remoteAddress;
		}

		@Override
		@Nullable
		protected SslInfo initSslInfo() {
			return this.sslInfo;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.body;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getNativeRequest() {
			return ServerHttpRequestDecorator.getNativeRequest(this.originalRequest);
		}

		@Override
		public String getId() {
			return this.originalRequest.getId();
		}
	}

}
