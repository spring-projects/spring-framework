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

package org.springframework.mock.http.server.reactive;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

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

	private Flux<DataBuffer> body = Flux.error(
			new IllegalStateException("The body is not set. " +
					"Did handling complete with success? Is a custom \"writeHandler\" configured?"));

	private Function<Flux<DataBuffer>, Mono<Void>> writeHandler = initDefaultWriteHandler();


	public MockServerHttpResponse() {
		super(new DefaultDataBufferFactory());
	}

	private Function<Flux<DataBuffer>, Mono<Void>> initDefaultWriteHandler() {
		return body -> {
			this.body = body.cache();
			return this.body.then();
		};
	}


	/**
	 * Return the request body, or an error stream if the body was never set
	 * or when {@link #setWriteHandler} is configured.
	 */
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * Shortcut method that delegates to {@link #getBody()} and then aggregates
	 * the data buffers and converts to a String using the charset of the
	 * Content-Type header or falling back on "UTF-8" by default.
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

	/**
	 * Configure a custom handler for writing the request body.
	 *
	 * <p>The default write handler consumes and caches the request body so it
	 * may be accessed subsequently, e.g. in test assertions. Use this property
	 * when the request body is an infinite stream.
	 *
	 * @param writeHandler the write handler to use returning {@code Mono<Void>}
	 * when the body has been "written" (i.e. consumed).
	 */
	public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
		Assert.notNull(writeHandler, "'writeHandler' is required");
		this.writeHandler = writeHandler;
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

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return this.writeHandler.apply(Flux.from(body));
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.empty())));
	}

}
