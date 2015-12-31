/*
 * Copyright 2002-2015 the original author or authors.
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

import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpRequest} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 */
public class ReactorServerHttpRequest implements ServerHttpRequest {

	private final HttpChannel<Buffer, ?> channel;

	private URI uri;

	private HttpHeaders headers;


	public ReactorServerHttpRequest(HttpChannel<Buffer, ?> request) {
		Assert.notNull("'request' must not be null.");
		this.channel = request;
	}


	public HttpChannel<Buffer, ?> getReactorChannel() {
		return this.channel;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.channel.method().getName());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				this.uri = new URI(this.channel.uri());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (String name : this.channel.headers().names()) {
				this.headers.put(name, this.channel.headers().getAll(name));
			}
		}
		return this.headers;
	}

	@Override
	public Publisher<ByteBuffer> getBody() {
		return Publishers.map(this.channel.input(), Buffer::byteBuffer);
	}

}
