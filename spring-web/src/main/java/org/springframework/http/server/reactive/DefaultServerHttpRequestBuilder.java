/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.LinkedList;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Package-private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

	private URI uri;

	private HttpHeaders httpHeaders;

	private String httpMethodValue;

	private final MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private String uriPath;

	@Nullable
	private String contextPath;

	@Nullable
	private SslInfo sslInfo;

	private Flux<DataBuffer> body;

	private final ServerHttpRequest originalRequest;


	public DefaultServerHttpRequestBuilder(ServerHttpRequest original) {
		Assert.notNull(original, "ServerHttpRequest is required");

		this.uri = original.getURI();
		this.httpMethodValue = original.getMethodValue();
		this.body = original.getBody();

		this.httpHeaders = HttpHeaders.writableHttpHeaders(original.getHeaders());

		this.cookies = new LinkedMultiValueMap<>(original.getCookies().size());
		copyMultiValueMap(original.getCookies(), this.cookies);

		this.originalRequest = original;
	}

	private static <K, V> void copyMultiValueMap(MultiValueMap<K,V> source, MultiValueMap<K,V> target) {
		source.forEach((key, value) -> target.put(key, new LinkedList<>(value)));
	}


	@Override
	public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
		this.httpMethodValue = httpMethod.name();
		return this;
	}

	@Override
	public ServerHttpRequest.Builder uri(URI uri) {
		this.uri = uri;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder path(String path) {
		Assert.isTrue(path.startsWith("/"), "The path does not have a leading slash.");
		this.uriPath = path;
		return this;
	}

	@Override
	public ServerHttpRequest.Builder contextPath(String contextPath) {
		this.contextPath = contextPath;
		return this;
	}

	@Override
	@Deprecated
	public ServerHttpRequest.Builder header(String key, String value) {
		this.httpHeaders.add(key, value);
		return this;
	}

	@Override
	public ServerHttpRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		Assert.notNull(headersConsumer, "'headersConsumer' must not be null");
		headersConsumer.accept(this.httpHeaders);
		return this;
	}

	@Override
	public ServerHttpRequest.Builder sslInfo(SslInfo sslInfo) {
		this.sslInfo = sslInfo;
		return this;
	}

	@Override
	public ServerHttpRequest build() {
		return new MutatedServerHttpRequest(getUriToUse(), this.contextPath, this.httpHeaders,
				this.httpMethodValue, this.cookies, this.sslInfo, this.body, this.originalRequest);
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

		private final String methodValue;

		private final MultiValueMap<String, HttpCookie> cookies;

		@Nullable
		private final InetSocketAddress remoteAddress;

		@Nullable
		private final SslInfo sslInfo;

		private final Flux<DataBuffer> body;

		private final ServerHttpRequest originalRequest;


		public MutatedServerHttpRequest(URI uri, @Nullable String contextPath,
				HttpHeaders headers, String methodValue, MultiValueMap<String, HttpCookie> cookies,
				@Nullable SslInfo sslInfo, Flux<DataBuffer> body, ServerHttpRequest originalRequest) {

			super(uri, contextPath, headers);
			this.methodValue = methodValue;
			this.cookies = cookies;
			this.remoteAddress = originalRequest.getRemoteAddress();
			this.sslInfo = sslInfo != null ? sslInfo : originalRequest.getSslInfo();
			this.body = body;
			this.originalRequest = originalRequest;
		}

		@Override
		public String getMethodValue() {
			return this.methodValue;
		}

		@Override
		protected MultiValueMap<String, HttpCookie> initCookies() {
			return this.cookies;
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
			return (T) this.originalRequest;
		}

		@Override
		public String getId() {
			return this.originalRequest.getId();
		}
	}

}
