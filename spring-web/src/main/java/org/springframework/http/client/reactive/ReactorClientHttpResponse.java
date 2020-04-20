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

package org.springframework.http.client.reactive;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see reactor.netty.http.client.HttpClient
 */
class ReactorClientHttpResponse implements ClientHttpResponse {

	private final NettyDataBufferFactory bufferFactory;

	private final HttpClientResponse response;

	private final NettyInbound inbound;

	private final AtomicBoolean rejectSubscribers = new AtomicBoolean();


	public ReactorClientHttpResponse(HttpClientResponse response, NettyInbound inbound, ByteBufAllocator alloc) {
		this.response = response;
		this.inbound = inbound;
		this.bufferFactory = new NettyDataBufferFactory(alloc);
	}


	@Override
	public Flux<DataBuffer> getBody() {
		return this.inbound.receive()
				.doOnSubscribe(s -> {
					if (this.rejectSubscribers.get()) {
						throw new IllegalStateException("The client response body can only be consumed once.");
					}
				})
				.doOnCancel(() ->
					// https://github.com/reactor/reactor-netty/issues/503
					// FluxReceive rejects multiple subscribers, but not after a cancel().
					// Subsequent subscribers after cancel() will not be rejected, but will hang instead.
					// So we need to intercept and reject them in that case.
					this.rejectSubscribers.set(true)
				)
				.map(byteBuf -> {
					byteBuf.retain();
					return this.bufferFactory.wrap(byteBuf);
				});
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.response.responseHeaders().entries().forEach(e -> headers.add(e.getKey(), e.getValue()));
		return headers;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.response.status().code();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.response.cookies().values().stream().flatMap(Collection::stream)
				.forEach(c ->
					result.add(c.name(), ResponseCookie.fromClientResponse(c.name(), c.value())
							.domain(c.domain())
							.path(c.path())
							.maxAge(c.maxAge())
							.secure(c.isSecure())
							.httpOnly(c.isHttpOnly())
							.build()));
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
				"status=" + getRawStatusCode() + '}';
	}

}
