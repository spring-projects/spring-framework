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

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

import static io.vertx.core.http.HttpHeaders.SET_COOKIE;
import static java.util.function.Function.identity;

/**
 * Adapt {@link ServerHttpResponse} to the Vertx {@link HttpServerResponse}.
 *
 * @author Yevhenii Melnyk
 * @since 5.0
 */
public class VertxServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServerResponse response;

	public VertxServerHttpResponse(HttpServerResponse response, DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		Assert.notNull(response, "'HttpServerResponse' must not be null.");
		this.response = response;
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
		return toBuffers(publisher)
				.doOnNext(response::write)
				.doOnComplete(response::end)
				.then();
	}

	private static Flux<Buffer> toBuffers(Publisher<? extends DataBuffer> dataBuffers) {
		return Flux.from(dataBuffers).map(b -> Buffer.buffer(NettyDataBufferFactory.toByteBuf(b)));
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
		// Vertx response flushes data when the internal buffer is full, so just flatten the stream
		return writeWithInternal(Flux.from(publisher).flatMap(identity()));
	}

	@Override
	protected void applyStatusCode() {
		HttpStatus statusCode = getStatusCode();
		if (statusCode != null) {
			response.setStatusCode(statusCode.value());
		}
	}

	@Override
	protected void applyHeaders() {
		HttpHeaders headers = getHeaders();
		if (!headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
			response.setChunked(true);
		}
		for (String name : headers.keySet()) {
			for (String value : headers.get(name)) {
				response.putHeader(name, value);
			}
		}
	}

	@Override
	protected void applyCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie nettyCookie = new DefaultCookie(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					nettyCookie.setMaxAge(httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(nettyCookie::setDomain);
				httpCookie.getPath().ifPresent(nettyCookie::setPath);
				nettyCookie.setSecure(httpCookie.isSecure());
				nettyCookie.setHttpOnly(httpCookie.isHttpOnly());
				response.headers()
						.add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(nettyCookie));
			}
		}
	}

}
