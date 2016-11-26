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
import java.util.Set;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static io.vertx.core.http.HttpHeaders.COOKIE;

/**
 * Adapt {@link ServerHttpRequest} to the Vertx {@link HttpServerRequest}.
 *
 * @author Yevhenii Melnyk
 * @since 5.0
 */
public class VertxServerHttpRequest extends AbstractServerHttpRequest {


	private final NettyDataBufferFactory bufferFactory;

	private final HttpServerRequest request;


	public VertxServerHttpRequest(HttpServerRequest request, NettyDataBufferFactory bufferFactory) {
		super(initUri(request), initHeaders(request));
		Assert.notNull(request, "'HttpServerRequest' must not be null.");
		this.request = request;
		this.bufferFactory = bufferFactory;
	}

	private static URI initUri(HttpServerRequest request) {
		Assert.notNull(request, "Vertx 'httpServerRequest' must not be null");
		try {
			URI uri = new URI(request.uri());
			SocketAddress remoteAddress = request.remoteAddress();
			return new URI(
					uri.getScheme(),
					uri.getUserInfo(),
					(remoteAddress != null ? remoteAddress.host() : null),
					(remoteAddress != null ? remoteAddress.port() : -1),
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
		}
	}

	private static HttpHeaders initHeaders(HttpServerRequest request) {
		Assert.notNull(request, "Vertx 'httpServerRequest' must not be null");
		HttpHeaders headers = new HttpHeaders();
		for (String name : request.headers().names()) {
			headers.put(name, request.headers().getAll(name));
		}
		return headers;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		String cookieHeader = request.headers().get(COOKIE);
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		if (cookieHeader != null) {
			Set<Cookie> nettyCookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
			for (Cookie cookie : nettyCookies) {
				HttpCookie httpCookie = new HttpCookie(cookie.name(), cookie.value());
				cookies.add(cookie.name(), httpCookie);
			}
		}
		return cookies;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(request.method().name());
	}

	@Override
	public Flux<DataBuffer> getBody() {
		EmitterProcessor<Buffer> stream = EmitterProcessor.<Buffer>create().connect();
		request.handler(stream::onNext);
		request.endHandler(e -> stream.onComplete());
		return stream.map(buffer -> bufferFactory.wrap(buffer.getByteBuf()));
	}

}