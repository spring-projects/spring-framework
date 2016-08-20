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

import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.http.HttpChannel;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 * @since 5.0
 */
public class ReactorServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpChannel channel;

	private final NettyDataBufferFactory dataBufferFactory;

	public ReactorServerHttpRequest(HttpChannel request,
			NettyDataBufferFactory dataBufferFactory) {
		Assert.notNull("'request' must not be null");
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		this.channel = request;
		this.dataBufferFactory = dataBufferFactory;
	}


	public HttpChannel getReactorChannel() {
		return this.channel;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.channel.method().name());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		return new URI(this.channel.uri());
	}

	@Override
	protected HttpHeaders initHeaders() {
		HttpHeaders headers = new HttpHeaders();
		for (String name : this.channel.headers().names()) {
			headers.put(name, this.channel.headers().getAll(name));
		}
		return headers;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		for (CharSequence name : this.channel.cookies().keySet()) {
			for (Cookie cookie : this.channel.cookies().get(name)) {
				HttpCookie httpCookie = new HttpCookie(name.toString(), cookie.value());
				cookies.add(name.toString(), httpCookie);
			}
		}
		return cookies;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.channel.receive().retain().map(this.dataBufferFactory::wrap);
	}

}
