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

import java.io.File;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.netty.http.HttpChannel;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorServerHttpResponse extends AbstractServerHttpResponse
		implements ZeroCopyHttpOutputMessage {

	private final HttpChannel channel;


	public ReactorServerHttpResponse(HttpChannel response,
			DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		Assert.notNull("'response' must not be null.");
		this.channel = response;
	}


	public HttpChannel getReactorChannel() {
		return this.channel;
	}


	@Override
	protected void writeStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			getReactorChannel().status(HttpResponseStatus.valueOf(statusCode.value()));
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<DataBuffer> publisher) {
		Publisher<ByteBuf> body = toByteBufs(publisher);
		return this.channel.send(body);
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(
			Publisher<Publisher<DataBuffer>> publisher) {
		Publisher<Publisher<ByteBuf>> body = Flux.from(publisher).
				map(ReactorServerHttpResponse::toByteBufs);
		return this.channel.sendAndFlush(body);
	}

	@Override
	protected void writeHeaders() {
		for (String name : getHeaders().keySet()) {
			for (String value : getHeaders().get(name)) {
				this.channel.responseHeaders().add(name, value);
			}
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new DefaultCookie(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge(httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(cookie::setDomain);
				httpCookie.getPath().ifPresent(cookie::setPath);
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.channel.addResponseCookie(cookie);
			}
		}
	}

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		return applyBeforeCommit().then(() -> this.channel.sendFile(file, position, count));
	}

	private static Publisher<ByteBuf> toByteBufs(Publisher<DataBuffer> dataBuffers) {
		return Flux.from(dataBuffers).
				map(NettyDataBufferFactory::toByteBuf);
	}



}
