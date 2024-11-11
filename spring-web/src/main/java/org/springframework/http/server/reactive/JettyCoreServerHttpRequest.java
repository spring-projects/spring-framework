/*
 * Copyright 2002-2024 the original author or authors.
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
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Request;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.JettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt an Eclipse Jetty {@link Request} to a {@link org.springframework.http.server.ServerHttpRequest}.
 *
 * @author Greg Wilkins
 * @author Arjen Poutsma
 * @since 6.2
 */
class JettyCoreServerHttpRequest extends AbstractServerHttpRequest {

	private final JettyDataBufferFactory dataBufferFactory;

	private final Request request;


	public JettyCoreServerHttpRequest(Request request, JettyDataBufferFactory dataBufferFactory) {
		super(HttpMethod.valueOf(request.getMethod()),
				request.getHttpURI().toURI(),
				request.getContext().getContextPath(),
				new HttpHeaders(new JettyHeadersAdapter(request.getHeaders())));
		this.dataBufferFactory = dataBufferFactory;
		this.request = request;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		List<org.eclipse.jetty.http.HttpCookie> httpCookies = Request.getCookies(this.request);
		if (httpCookies.isEmpty()) {
			return CollectionUtils.toMultiValueMap(Collections.emptyMap());
		}
		MultiValueMap<String, HttpCookie> cookies =new LinkedMultiValueMap<>();
		for (org.eclipse.jetty.http.HttpCookie c : httpCookies) {
			cookies.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
		}
		return cookies;
	}

	@Override
	@Nullable
	public SslInfo initSslInfo() {
		if (this.request.getConnectionMetaData().isSecure() &&
				this.request.getAttribute(EndPoint.SslSessionData.ATTRIBUTE) instanceof EndPoint.SslSessionData sessionData) {
			return new DefaultSslInfo(sessionData.sslSessionId(), sessionData.peerCertificates());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	@Override
	protected String initId() {
		return this.request.getId();
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		SocketAddress localAddress = this.request.getConnectionMetaData().getLocalSocketAddress();
		return localAddress instanceof InetSocketAddress inet ? inet : null;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		SocketAddress remoteAddress = this.request.getConnectionMetaData().getRemoteSocketAddress();
		return remoteAddress instanceof InetSocketAddress inet ? inet : null;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		// We access the request body as a Flow.Publisher, which is wrapped as an org.reactivestreams.Publisher and
		// then wrapped as a Flux.
		return Flux.from(FlowAdapters.toPublisher(Content.Source.asPublisher(this.request)))
				.map(this.dataBufferFactory::wrap);
	}

}
