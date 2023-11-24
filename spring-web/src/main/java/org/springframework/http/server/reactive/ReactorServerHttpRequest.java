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
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.ssl.SslHandler;
import org.apache.commons.logging.Log;
import reactor.core.publisher.Flux;
import reactor.netty.ChannelOperationsId;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerRequest;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor {@link HttpServerRequest}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactorServerHttpRequest extends AbstractServerHttpRequest {

	private static final Log logger = HttpLogging.forLogName(ReactorServerHttpRequest.class);


	private static final AtomicLong logPrefixIndex = new AtomicLong();


	private final HttpServerRequest request;

	private final NettyDataBufferFactory bufferFactory;


	public ReactorServerHttpRequest(HttpServerRequest request, NettyDataBufferFactory bufferFactory)
			throws URISyntaxException {

		super(HttpMethod.valueOf(request.method().name()), ReactorUriHelper.createUri(request), "",
				new Netty4HeadersAdapter(request.requestHeaders()));
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		this.request = request;
		this.bufferFactory = bufferFactory;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		for (CharSequence name : this.request.cookies().keySet()) {
			for (Cookie cookie : this.request.cookies().get(name)) {
				HttpCookie httpCookie = new HttpCookie(name.toString(), cookie.value());
				cookies.add(name.toString(), httpCookie);
			}
		}
		return cookies;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.request.hostAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.request.remoteAddress();
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		Channel channel = ((Connection) this.request).channel();
		SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
		if (sslHandler == null && channel.parent() != null) { // HTTP/2
			sslHandler = channel.parent().pipeline().get(SslHandler.class);
		}
		if (sslHandler != null) {
			SSLSession session = sslHandler.engine().getSession();
			return new DefaultSslInfo(session);
		}
		return null;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.request.receive().retain().map(this.bufferFactory::wrap);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	@Override
	@Nullable
	protected String initId() {
		if (this.request instanceof Connection connection) {
			return connection.channel().id().asShortText() +
					"-" + logPrefixIndex.incrementAndGet();
		}
		return null;
	}

	@Override
	protected String initLogPrefix() {
		String id = null;
		if (this.request instanceof ChannelOperationsId operationsId) {
			id = (logger.isDebugEnabled() ? operationsId.asLongText() : operationsId.asShortText());
		}
		if (id != null) {
			return id;
		}
		if (this.request instanceof Connection connection) {
			return connection.channel().id().asShortText() +
					"-" + logPrefixIndex.incrementAndGet();
		}
		return getId();
	}

}
