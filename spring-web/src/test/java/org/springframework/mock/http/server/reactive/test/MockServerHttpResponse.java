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

package org.springframework.mock.http.server.reactive.test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Mock implementation of {@link ServerHttpResponse}.
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerHttpResponse extends AbstractServerHttpResponse {

	private Flux<DataBuffer> body;


	public MockServerHttpResponse() {
		super(new DefaultDataBufferFactory());
	}


	/**
	 * Return the output Publisher used to write to the response.
	 */
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * Return the response body aggregated and converted to a String using the
	 * charset of the Content-Type response or otherwise as "UTF-8".
	 */
	public Mono<String> getBodyAsString() {
		Charset charset = getCharset();
		return getBody()
				.reduce(bufferFactory().allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				})
				.map(buffer -> bufferToString(buffer, charset));
	}

	private static String bufferToString(DataBuffer buffer, Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		byte[] bytes = new byte[buffer.readableByteCount()];
		buffer.read(bytes);
		return new String(bytes, charset);
	}

	private Charset getCharset() {
		Charset charset = null;
		MediaType contentType = getHeaders().getContentType();
		if (contentType != null) {
			charset = contentType.getCharset();
		}
		return (charset != null ? charset : StandardCharsets.UTF_8);
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		this.body = Flux.from(body);
		return Mono.empty();
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWithInternal(Flux.from(body).flatMap(Flux::from));
	}

	@Override
	protected void applyStatusCode() {
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
	}

}
