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
import java.nio.ByteBuffer;

import reactor.core.publisher.Flux;
import reactor.io.buffer.Buffer;
import reactor.io.netty.http.HttpChannel;
import reactor.io.netty.http.model.Cookie;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 */
public class ReactorServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpChannel<Buffer, ?> channel;

	private final DataBufferAllocator allocator;

	public ReactorServerHttpRequest(HttpChannel<Buffer, ?> request,
			DataBufferAllocator allocator) {
		Assert.notNull("'request' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");
		this.channel = request;
		this.allocator = allocator;
	}


	public HttpChannel<Buffer, ?> getReactorChannel() {
		return this.channel;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.channel.method().getName());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		return new URI(this.channel.uri());
	}

	@Override
	protected void initHeaders(HttpHeaders headers) {
		for (String name : this.channel.headers().names()) {
			headers.put(name, this.channel.headers().getAll(name));
		}
	}

	@Override
	protected void initCookies(MultiValueMap<String, HttpCookie> cookies) {
		for (String name : this.channel.cookies().keySet()) {
			for (Cookie cookie : this.channel.cookies().get(name)) {
				HttpCookie httpCookie = new HttpCookie(name, cookie.value());
				cookies.add(name, httpCookie);
			}
		}
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.channel.input()).map(bytes -> {
			ByteBuffer byteBuffer = bytes.byteBuffer();
			return allocator.wrap(byteBuffer);
		});
	}

}
