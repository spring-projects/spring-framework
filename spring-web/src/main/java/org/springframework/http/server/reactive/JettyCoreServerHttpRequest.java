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
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Adapt an Eclipse Jetty {@link Request} to a {@link org.springframework.http.server.ServerHttpRequest}.
 *
 * @author Greg Wilkins
 * @since 6.2
 */
class JettyCoreServerHttpRequest extends AbstractServerHttpRequest {
	private static final MultiValueMap<String, String> EMPTY_QUERY = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	private static final MultiValueMap<String, HttpCookie> EMPTY_COOKIES = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	private final DataBufferFactory dataBufferFactory;

	private final Request request;

	public JettyCoreServerHttpRequest(DataBufferFactory dataBufferFactory, Request request) {
		super(HttpMethod.valueOf(request.getMethod()),
				request.getHttpURI().toURI(),
				request.getContext().getContextPath(),
				new HttpHeaders(new JettyHeadersAdapter(request.getHeaders())));
		this.dataBufferFactory = dataBufferFactory;
		this.request = request;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// We access the request body as a Flow.Publisher, which is wrapped as an org.reactivestreams.Publisher and
		// then wrapped as a Flux.
		return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(this.request))).map(this::chunkToDataBuffer);
	}

	private DataBuffer chunkToDataBuffer(Content.Chunk chunk) {
		return new JettyRetainedDataBuffer(this.dataBufferFactory, chunk);
	}

	@Override
	protected String initId() {
		return this.request.getId();
	}

	@Override
	protected MultiValueMap<String, String> initQueryParams() {
		String query = this.request.getHttpURI().getQuery();
		if (StringUtil.isBlank(query)) {
			return EMPTY_QUERY;
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		Matcher matcher = QUERY_PATTERN.matcher(query);
		while (matcher.find()) {
			String name = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
			String eq = matcher.group(2);
			String value = matcher.group(3);
			value = (value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : (StringUtils.hasLength(eq) ? "" : null));
			map.add(name, value);
		}
		return map;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		List<org.eclipse.jetty.http.HttpCookie> httpCookies = Request.getCookies(this.request);
		if (httpCookies.isEmpty()) {
			return EMPTY_COOKIES;
		}

		MultiValueMap<String, HttpCookie> map =new LinkedMultiValueMap<>();
		for (org.eclipse.jetty.http.HttpCookie c : httpCookies) {
			map.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
		}

		return map;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.request.getConnectionMetaData().getLocalSocketAddress() instanceof InetSocketAddress inet ? inet : null;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.request.getConnectionMetaData().getRemoteSocketAddress() instanceof InetSocketAddress inet ? inet : null;
	}

	@Override
	@Nullable
	public SslInfo initSslInfo() {
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
