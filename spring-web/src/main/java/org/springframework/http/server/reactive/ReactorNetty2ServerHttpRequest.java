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
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import io.netty5.channel.Channel;
import io.netty5.handler.codec.http.HttpHeaderNames;
import io.netty5.handler.codec.http.headers.HttpCookiePair;
import io.netty5.handler.ssl.SslHandler;
import org.apache.commons.logging.Log;
import reactor.core.publisher.Flux;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.Connection;
import reactor.netty5.http.server.HttpServerRequest;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor {@link HttpServerRequest}.
 *
 * <p>This class is based on {@link ReactorServerHttpRequest}.
 *
 * @author Violeta Georgieva
 * @author Sebastien Deleuze
 * @since 6.0
 */
class ReactorNetty2ServerHttpRequest extends AbstractServerHttpRequest {

	private static final Log logger = HttpLogging.forLogName(ReactorNetty2ServerHttpRequest.class);


	private static final AtomicLong logPrefixIndex = new AtomicLong();


	private final HttpServerRequest request;

	private final Netty5DataBufferFactory bufferFactory;


	public ReactorNetty2ServerHttpRequest(HttpServerRequest request, Netty5DataBufferFactory bufferFactory)
			throws URISyntaxException {

		super(HttpMethod.valueOf(request.method().name()), initUri(request), "",
				new Netty5HeadersAdapter(request.requestHeaders()));
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		this.request = request;
		this.bufferFactory = bufferFactory;
	}

	private static URI initUri(HttpServerRequest request) throws URISyntaxException {
		Assert.notNull(request, "HttpServerRequest must not be null");
		return new URI(resolveBaseUrl(request) + resolveRequestUri(request));
	}

	private static URI resolveBaseUrl(HttpServerRequest request) throws URISyntaxException {
		String scheme = getScheme(request);

		InetSocketAddress hostAddress = request.hostAddress();
		if (hostAddress != null) {
			return new URI(scheme, null, hostAddress.getHostString(), hostAddress.getPort(), null, null, null);
		}

		CharSequence charSequence = request.requestHeaders().get(HttpHeaderNames.HOST);
		if (charSequence != null) {
			String header = charSequence.toString();
			final int portIndex;
			if (header.startsWith("[")) {
				portIndex = header.indexOf(':', header.indexOf(']'));
			}
			else {
				portIndex = header.indexOf(':');
			}
			if (portIndex != -1) {
				try {
					return new URI(scheme, null, header.substring(0, portIndex),
							Integer.parseInt(header, portIndex + 1, header.length(), 10), null, null, null);
				}
				catch (NumberFormatException ex) {
					throw new URISyntaxException(header, "Unable to parse port", portIndex);
				}
			}
			else {
				return new URI(scheme, header, null, null);
			}
		}

		throw new IllegalStateException("Neither local hostAddress nor HOST header available");
	}

	private static String getScheme(HttpServerRequest request) {
		return request.scheme();
	}

	private static String resolveRequestUri(HttpServerRequest request) {
		String uri = request.uri();
		for (int i = 0; i < uri.length(); i++) {
			char c = uri.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				break;
			}
			if (c == ':' && (i + 2 < uri.length())) {
				if (uri.charAt(i + 1) == '/' && uri.charAt(i + 2) == '/') {
					for (int j = i + 3; j < uri.length(); j++) {
						c = uri.charAt(j);
						if (c == '/' || c == '?' || c == '#') {
							return uri.substring(j);
						}
					}
					return "";
				}
			}
		}
		return uri;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		for (CharSequence name : this.request.allCookies().keySet()) {
			for (HttpCookiePair cookie : this.request.allCookies().get(name)) {
				CharSequence cookieValue = cookie.value();
				HttpCookie httpCookie = new HttpCookie(name.toString(), cookieValue != null ? cookieValue.toString() : null);
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
		return this.request.receive().transferOwnership().map(this.bufferFactory::wrap);
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
