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
package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.http.client.HttpClientRequest;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * A Reactor Netty based implementation of {@link WebSocketClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketClient implements WebSocketClient {

	private final HttpClient httpClient;


	public ReactorNettyWebSocketClient() {
		this.httpClient = HttpClient.create();
	}

	public ReactorNettyWebSocketClient(Consumer<? super HttpClientOptions> clientOptions) {
		this.httpClient = HttpClient.create(clientOptions);
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {

		// We have to store the NettyOutbound fow now..
		// The alternative HttpClientResponse#receiveWebSocket does not work at present
		AtomicReference<NettyOutbound> outboundRef = new AtomicReference<>();

		return this.httpClient
				.get(url.toString(), request -> {
					addHeaders(request, headers);
					NettyOutbound outbound = request.sendWebsocket();
					outboundRef.set(outbound);
					return outbound;
				})
				.then(inbound -> {
					ByteBufAllocator allocator = inbound.channel().alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);
					NettyOutbound outbound = outboundRef.get();
					HandshakeInfo info = new HandshakeInfo(url, headers, Mono.empty());
					WebSocketSession session = new ReactorNettyWebSocketSession(inbound, outbound, info,  factory);
					return handler.handle(session);
				});
	}

	private void addHeaders(HttpClientRequest request, HttpHeaders headers) {
		headers.entrySet().stream()
				.forEach(e -> request.requestHeaders().set(e.getKey(), e.getValue()));
	}

}
