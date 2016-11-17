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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.http.server.HttpServerRequest;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class ReactorServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerRequest request;

	private final NettyDataBufferFactory bufferFactory;


	public ReactorServerHttpRequest(HttpServerRequest request, NettyDataBufferFactory bufferFactory) {
		super(initUri(request), initHeaders(request));
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		this.request = request;
		this.bufferFactory = bufferFactory;
	}

	private static URI initUri(HttpServerRequest channel) {
		Assert.notNull("'channel' must not be null");
		try {
			URI uri = new URI(channel.uri());
			InetSocketAddress remoteAddress = channel.remoteAddress();
			return new URI(
					uri.getScheme(),
					uri.getUserInfo(),
					(remoteAddress != null ? remoteAddress.getHostString() : null),
					(remoteAddress != null ? remoteAddress.getPort() : -1),
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
		}
	}

	private static HttpHeaders initHeaders(HttpServerRequest channel) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : channel.requestHeaders().names()) {
			headers.put(name, channel.requestHeaders().getAll(name));
		}
		return headers;
	}


	public HttpServerRequest getReactorRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.method().name());
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
	public Flux<DataBuffer> getBody() {
		return this.request.receive().retain().map(this.bufferFactory::wrap);
	}

}
