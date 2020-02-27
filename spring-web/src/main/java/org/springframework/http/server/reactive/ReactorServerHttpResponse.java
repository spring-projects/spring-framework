/*
 * Copyright 2002-2020 the original author or authors.
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

import java.nio.file.Path;
import java.util.List;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the {@link HttpServerResponse}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactorServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {

	private final HttpServerResponse response;


	public ReactorServerHttpResponse(HttpServerResponse response, DataBufferFactory bufferFactory) {
		super(bufferFactory, new HttpHeaders(new NettyHeadersAdapter(response.responseHeaders())));
		Assert.notNull(response, "HttpServerResponse must not be null");
		this.response = response;
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	@Override
	public HttpStatus getStatusCode() {
		HttpStatus status = super.getStatusCode();
		return (status != null ? status : HttpStatus.resolve(this.response.status().code()));
	}

	@Override
	public Integer getRawStatusCode() {
		Integer status = super.getRawStatusCode();
		return (status != null ? status : this.response.status().code());
	}

	@Override
	protected void applyStatusCode() {
		Integer status = super.getRawStatusCode();
		if (status != null) {
			this.response.status(status);
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
		return this.response.send(toByteBufs(publisher)).then();
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
		return this.response.sendGroups(Flux.from(publisher).map(this::toByteBufs)).then();
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		// Netty Cookie doesn't support sameSite. When this is resolved, we can adapt to it again:
		// https://github.com/netty/netty/issues/8161
		for (List<ResponseCookie> cookies : getCookies().values()) {
			for (ResponseCookie cookie : cookies) {
				this.response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		return doCommit(() -> this.response.sendFile(file, position, count).then());
	}

	private Publisher<ByteBuf> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
		return dataBuffers instanceof Mono ?
				Mono.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf) :
				Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
	}

}
