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
import java.util.Objects;
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
 * Adapt an Eclipse Jetty {@link Request} to a {@link org.springframework.http.server.ServerHttpRequest}
 *
 * @author Greg Wilkins
 * @since 6.2
 */
class JettyCoreServerHttpRequest implements ServerHttpRequest {
	private final static MultiValueMap<String, String> EMPTY_QUERY = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	private final static MultiValueMap<String, HttpCookie> EMPTY_COOKIES = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

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
		headers = new HttpHeaders(new JettyHeadersAdapter(request.getHeaders()));
		path = RequestPath.parse(request.getHttpURI().getPath(), request.getContext().getContextPath());
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(request.getMethod());
	}

	@Override
	public URI getURI() {
		if (uri == null)
			uri = request.getHttpURI().toURI();
		return uri;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// We access the request body as a Flow.Publisher, which is wrapped as an org.reactivestreams.Publisher and
		// then wrapped as a Flux.   The chunks are converted to RetainedDataBuffers with wrapping and can be
		// retained within a call to onNext.
		return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(request))).map(this::wrap);
	}

	private JettyRetainedDataBuffer wrap(Content.Chunk chunk) {
		return new JettyRetainedDataBuffer(dataBufferFactory.wrap(chunk.getByteBuffer()), chunk);
	}

	@Override
	public String getId() {
		return request.getId();
	}

	@Override
	public RequestPath getPath() {
		return path;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (queryParameters == null) {
			String query = request.getHttpURI().getQuery();
			if (StringUtil.isBlank(query))
				queryParameters = EMPTY_QUERY;
			else {
				queryParameters = new LinkedMultiValueMap<>();
				Matcher matcher = QUERY_PATTERN.matcher(query);
				while (matcher.find()) {
					String name = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
					String eq = matcher.group(2);
					String value = matcher.group(3);
					value = (value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : (StringUtils.hasLength(eq) ? "" : null));
					queryParameters.add(name, value);
				}
			}
		}
		return queryParameters;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (cookies == null) {
			List<org.eclipse.jetty.http.HttpCookie> httpCookies = Request.getCookies(request);
			if (httpCookies.isEmpty())
				cookies = EMPTY_COOKIES;
			else {
				cookies = new LinkedMultiValueMap<>();
				for (org.eclipse.jetty.http.HttpCookie c : httpCookies)
					cookies.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
				cookies = CollectionUtils.unmodifiableMultiValueMap(cookies);
			}
		}
		return cookies;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return request.getConnectionMetaData().getLocalSocketAddress() instanceof InetSocketAddress inet
				? inet : null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return request.getConnectionMetaData().getRemoteSocketAddress() instanceof InetSocketAddress inet
				? inet : null;
	}

	@Override
	public SslInfo getSslInfo() {
		if (request.getConnectionMetaData().isSecure() && request.getAttribute(EndPoint.SslSessionData.ATTRIBUTE) instanceof EndPoint.SslSessionData sslSessionData) {
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

	@Override
	public Builder mutate() {
		return new DefaultServerHttpRequestBuilder(this.getURI(),
						new HttpHeaders(new JettyHeadersAdapter(HttpFields.build(request.getHeaders()))),
						this.getMethod(),
						this.getPath().contextPath().value(),
						this.getRemoteAddress(),
						this.getBody(),
						Objects.requireNonNull(this, "ServerHttpRequest is required"));
	}
}
