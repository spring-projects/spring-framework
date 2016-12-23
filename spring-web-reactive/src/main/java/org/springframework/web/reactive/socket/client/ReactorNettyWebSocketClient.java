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
import java.util.function.Consumer;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;

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
public class ReactorNettyWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

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

		String[] protocols = beforeHandshake(url, headers, handler);
		// TODO: https://github.com/reactor/reactor-netty/issues/20

		return this.httpClient
				.get(url.toString(), request -> {
					addRequestHeaders(request, headers);
					return request.sendWebsocket();
				})
				.then(response -> {
					HttpHeaders responseHeaders = getResponseHeaders(response);
					HandshakeInfo info = afterHandshake(url, responseHeaders);

					ByteBufAllocator allocator = response.channel().alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);

					return response.receiveWebsocket((in, out) -> {
						WebSocketSession session = new ReactorNettyWebSocketSession(in, out, info, factory);
						return handler.handle(session);
					});
				});
	}

	private void addRequestHeaders(HttpClientRequest request, HttpHeaders headers) {
		headers.keySet().stream()
				.forEach(key -> headers.get(key).stream()
						.forEach(value -> request.addHeader(key, value)));
	}

	private HttpHeaders getResponseHeaders(HttpClientResponse response) {
		HttpHeaders headers = new HttpHeaders();
		response.responseHeaders().forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, response.responseHeaders().getAll(name));
		});
		return headers;
	}

}
