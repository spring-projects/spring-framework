/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.http.client.HttpClientResponse;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with Reactor Netty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private final HttpClient httpClient;


	/**
	 * Default constructor.
	 */
	public ReactorNettyWebSocketClient() {
		this(options -> {});
	}

	/**
	 * Constructor that accepts an {@code HttpClientOptions.Builder} consumer
	 * to supply to {@link HttpClient#create(Consumer)}.
	 */
	public ReactorNettyWebSocketClient(Consumer<? super HttpClientOptions.Builder> clientOptions) {
		this.httpClient = HttpClient.create(clientOptions);
	}


	/**
	 * Return the configured {@link HttpClient}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		List<String> protocols = beforeHandshake(url, headers, handler);

		return getHttpClient()
				.ws(url.toString(),
						nettyHeaders -> setNettyHeaders(headers, nettyHeaders),
						StringUtils.collectionToCommaDelimitedString(protocols))
				.flatMap(response -> {
					HandshakeInfo info = afterHandshake(url, toHttpHeaders(response));
					ByteBufAllocator allocator = response.channel().alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);
					return response.receiveWebsocket((in, out) -> {
						WebSocketSession session = new ReactorNettyWebSocketSession(in, out, info, factory);
						return handler.handle(session);
					});
				});
	}

	private void setNettyHeaders(HttpHeaders headers, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
		headers.forEach(nettyHeaders::set);
	}

	private HttpHeaders toHttpHeaders(HttpClientResponse response) {
		HttpHeaders headers = new HttpHeaders();
		response.responseHeaders().forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, response.responseHeaders().getAll(name));
		});
		return headers;
	}

}
