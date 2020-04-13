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

package org.springframework.web.reactive.socket.server.upgrade;

import java.net.URI;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;
import reactor.netty.http.server.WebsocketServerSpec.Builder;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with Reactor Netty.
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private Builder websocketServerSpec = WebsocketServerSpec.builder();

	/**
	 * Configure websocket specification {@link WebsocketServerSpec}
	 * @param websocketServerSpecBuilder websocket server parameters (ping, compression, maxFramePayloadLength)
	 * @since 5.2.6
	 */
	public void setWebsocketServerSpecBuilder(final Builder websocketServerSpecBuilder) {
		this.websocketServerSpec = websocketServerSpecBuilder;
	}

	@Override
	@SuppressWarnings("deprecation")
	public Mono<Void> upgrade(
			ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory
	) {

		ServerHttpResponse response = exchange.getResponse();
		HttpServerResponse reactorResponse = getNativeResponse(response);
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();

		// Trigger WebFlux preCommit actions and upgrade
		return response.setComplete()
				.then(Mono.defer(() -> reactorResponse.sendWebsocket(
						(in, out) -> {
							ReactorNettyWebSocketSession session =
									new ReactorNettyWebSocketSession(
											in,
											out,
											handshakeInfo,
											bufferFactory,
											this.websocketServerSpec.build()
													.maxFramePayloadLength());
							URI uri = exchange.getRequest().getURI();
							return handler.handle(session)
									.checkpoint(uri + " [ReactorNettyRequestUpgradeStrategy]");
						}, this.websocketServerSpec.protocols(subProtocol).build())));
	}

	private static HttpServerResponse getNativeResponse(ServerHttpResponse response) {
		if (response instanceof AbstractServerHttpResponse) {
			return ((AbstractServerHttpResponse) response).getNativeResponse();
		} else if (response instanceof ServerHttpResponseDecorator) {
			return getNativeResponse(((ServerHttpResponseDecorator) response).getDelegate());
		} else {
			throw new IllegalArgumentException(
					"Couldn't find native response in " + response.getClass().getName());
		}
	}
}
