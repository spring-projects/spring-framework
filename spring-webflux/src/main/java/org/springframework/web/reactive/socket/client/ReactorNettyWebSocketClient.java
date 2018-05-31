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
package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

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
		this(HttpClient.create());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder.
	 */
	public ReactorNettyWebSocketClient(HttpClient httpClient) {
		this.httpClient = httpClient;
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
				.headers(nettyHeaders -> setNettyHeaders(headers, nettyHeaders))
				.websocket(StringUtils.collectionToCommaDelimitedString(protocols))
				.uri(url.toString())
				.handle((in, out) -> {
					HandshakeInfo info = afterHandshake(url, toHttpHeaders(in.headers()));
					ByteBufAllocator allocator = out.alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);
					WebSocketSession session = new ReactorNettyWebSocketSession(in, out, info, factory);
					return handler.handle(session);
				})
				.next();
	}

	private void setNettyHeaders(HttpHeaders headers, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
		headers.forEach(nettyHeaders::set);
	}

	private HttpHeaders toHttpHeaders(io.netty.handler.codec.http.HttpHeaders responseHeaders) {
		HttpHeaders headers = new HttpHeaders();
		responseHeaders.forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, responseHeaders.getAll(name));
		});
		return headers;
	}

}
