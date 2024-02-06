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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.StringUtil;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import static org.springframework.http.server.reactive.AbstractServerHttpRequest.QUERY_PATTERN;

/**
 * Adapt an Eclipse Jetty {@link Request} to a {@link org.springframework.http.server.ServerHttpRequest}.
 *
 * @author Greg Wilkins
 * @since 6.2
 */
class JettyCoreServerHttpRequest implements ServerHttpRequest {
	private static final MultiValueMap<String, String> EMPTY_QUERY = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	private static final MultiValueMap<String, HttpCookie> EMPTY_COOKIES = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	private final DataBufferFactory dataBufferFactory;

	private final Request request;

	private final HttpHeaders headers;

	private final RequestPath path;

	@Nullable
	private URI uri;

	@Nullable
	MultiValueMap<String, String> queryParameters;

	@Nullable
	private MultiValueMap<String, HttpCookie> cookies;

	public JettyCoreServerHttpRequest(DataBufferFactory dataBufferFactory, Request request) {
		this.dataBufferFactory = dataBufferFactory;
		this.request = request;
		this.headers = new HttpHeaders(new JettyHeadersAdapter(request.getHeaders()));
		this.path = RequestPath.parse(request.getHttpURI().getPath(), request.getContext().getContextPath());
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.getMethod());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			this.uri = this.request.getHttpURI().toURI();
		}
		return this.uri;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// We access the request body as a Flow.Publisher, which is wrapped as an org.reactivestreams.Publisher and
		// then wrapped as a Flux.
		return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(this.request)))
				.map(this::wrap);
	}

	private DataBuffer wrap(Content.Chunk chunk) {
		return new JettyRetainedDataBuffer(this.dataBufferFactory, chunk);
	}

	@Override
	public String getId() {
		return this.request.getId();
	}

	@Override
	public RequestPath getPath() {
		return this.path;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (this.queryParameters == null) {
			String query = this.request.getHttpURI().getQuery();
			if (StringUtil.isBlank(query)) {
				this.queryParameters = EMPTY_QUERY;
			}
			else {
				this.queryParameters = new LinkedMultiValueMap<>();
				Matcher matcher = QUERY_PATTERN.matcher(query);
				while (matcher.find()) {
					String name = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
					String eq = matcher.group(2);
					String value = matcher.group(3);
					value = (value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : (StringUtils.hasLength(eq) ? "" : null));
					this.queryParameters.add(name, value);
				}
			}
		}
		return this.queryParameters;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (this.cookies == null) {
			List<org.eclipse.jetty.http.HttpCookie> httpCookies = Request.getCookies(this.request);
			if (httpCookies.isEmpty()) {
				this.cookies = EMPTY_COOKIES;
			}
			else {
				this.cookies = new LinkedMultiValueMap<>();
				for (org.eclipse.jetty.http.HttpCookie c : httpCookies) {
					this.cookies.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
				}
				this.cookies = CollectionUtils.unmodifiableMultiValueMap(this.cookies);
			}
		}
		return this.cookies;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.request.getConnectionMetaData().getLocalSocketAddress() instanceof InetSocketAddress inet
				? inet : null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.request.getConnectionMetaData().getRemoteSocketAddress() instanceof InetSocketAddress inet
				? inet : null;
	}

	@Override
	public SslInfo getSslInfo() {
		if (this.request.getConnectionMetaData().isSecure() && this.request.getAttribute(EndPoint.SslSessionData.ATTRIBUTE) instanceof EndPoint.SslSessionData sslSessionData) {
			return new SslInfo() {
				@Override
				public String getSessionId() {
					return sslSessionData.sslSessionId();
				}

				@Override
				public X509Certificate[] getPeerCertificates() {
					return sslSessionData.peerCertificates();
				}
			};
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	@Override
	public Builder mutate() {
		return new DefaultServerHttpRequestBuilder(this.getURI(),
				new HttpHeaders(new JettyHeadersAdapter(HttpFields.build(this.request.getHeaders()))),
				this.getMethod(),
				this.getPath().contextPath().value(),
				this.getRemoteAddress(),
				this.getBody(),
				this);
	}
}
