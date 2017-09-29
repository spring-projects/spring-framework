/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.io.File;
import java.net.URI;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientRequest;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ZeroCopyHttpOutputMessage;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see reactor.ipc.netty.http.client.HttpClient
 */
class ReactorClientHttpRequest extends AbstractClientHttpRequest implements ZeroCopyHttpOutputMessage {

	private final HttpMethod httpMethod;

	private final URI uri;

	private final HttpClientRequest httpRequest;

	private final NettyDataBufferFactory bufferFactory;


	public ReactorClientHttpRequest(HttpMethod httpMethod, URI uri,
			HttpClientRequest httpRequest) {
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.httpRequest = httpRequest.failOnClientError(false).failOnServerError(false);
		this.bufferFactory = new NettyDataBufferFactory(httpRequest.alloc());
	}


	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> this.httpRequest
				.send(Flux.from(body).map(NettyDataBufferFactory::toByteBuf)).then());
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		Publisher<Publisher<ByteBuf>> byteBufs = Flux.from(body).map(ReactorClientHttpRequest::toByteBufs);
		return doCommit(() -> this.httpRequest.sendGroups(byteBufs).then());
	}

	private static Publisher<ByteBuf> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
		return Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
	}

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		return doCommit(() -> this.httpRequest.sendFile(file.toPath(), position, count).then());
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> httpRequest.sendHeaders().then());
	}

	@Override
	protected void applyHeaders() {
		getHeaders().entrySet().forEach(e -> this.httpRequest.requestHeaders().set(e.getKey(), e.getValue()));
	}

	@Override
	protected void applyCookies() {
		getCookies().values().stream().flatMap(Collection::stream)
				.map(cookie -> new DefaultCookie(cookie.getName(), cookie.getValue()))
				.forEach(this.httpRequest::addCookie);
	}

}
