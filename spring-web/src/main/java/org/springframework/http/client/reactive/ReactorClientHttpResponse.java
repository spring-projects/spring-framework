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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import reactor.core.publisher.Flux;
import reactor.netty.Connection;
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

	private final HttpClientResponse response;

	private final NettyInbound inbound;

	private final NettyDataBufferFactory bufferFactory;

	private final Connection connection;

	private final HttpHeaders headers;

	// 0 - not subscribed, 1 - subscribed, 2 - cancelled
	private final AtomicInteger state = new AtomicInteger(0);


	/**
	 * Constructor that matches the inputs from
	 * {@link reactor.netty.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)}.
	 * @since 5.3
	 */
	public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
		this.response = response;
		this.inbound = connection.inbound();
		this.bufferFactory = new NettyDataBufferFactory(connection.outbound().alloc());
		this.connection = connection;
		MultiValueMap<String, String> adapter = new NettyHeadersAdapter(response.responseHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
	}


	@Override
	public Flux<DataBuffer> getBody() {
		return this.inbound.receive()
				.doOnSubscribe(s -> {
					if (!this.state.compareAndSet(0, 1)) {
						// https://github.com/reactor/reactor-netty/issues/503
						// FluxReceive rejects multiple subscribers, but not after a cancel().
						// Subsequent subscribers after cancel() will not be rejected, but will hang instead.
						// So we need to reject once in cancelled state.
						if (this.state.get() == 2) {
							throw new IllegalStateException("The client response body can only be consumed once.");
						}
					}
				})
				.doOnCancel(() -> this.state.compareAndSet(1, 2))
				.map(byteBuf -> {
					byteBuf.retain();
					return this.bufferFactory.wrap(byteBuf);
				});
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
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

	/**
	 * For use by {@link ReactorClientHttpConnector}.
	 */
	boolean bodyNotSubscribed() {
		return this.state.get() == 0;
	}

	/**
	 * For use by {@link ReactorClientHttpConnector}.
	 */
	Connection getConnection() {
		return this.connection;
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
				"status=" + getRawStatusCode() + '}';
	}

}
