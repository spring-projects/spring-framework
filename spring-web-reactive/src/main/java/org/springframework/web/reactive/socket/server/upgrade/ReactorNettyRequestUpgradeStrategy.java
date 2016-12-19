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
package org.springframework.web.reactive.socket.server.upgrade;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ReactorServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with Reactor Netty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler) {

		ReactorServerHttpResponse response = (ReactorServerHttpResponse) exchange.getResponse();
		HandshakeInfo handshakeInfo = getHandshakeInfo(exchange);
		NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();

		String protocols = StringUtils.arrayToCommaDelimitedString(getSubProtocols(handler));
		protocols = (StringUtils.hasText(protocols) ? protocols : null);

		return response.getReactorResponse().sendWebsocket(protocols,
				(inbound, outbound) -> handler.handle(
						new ReactorNettyWebSocketSession(inbound, outbound, handshakeInfo, bufferFactory)));
	}

	private HandshakeInfo getHandshakeInfo(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		return new HandshakeInfo(request.getURI(), request.getHeaders(), exchange.getPrincipal());
	}

	private static String[] getSubProtocols(WebSocketHandler webSocketHandler) {
		List<String> subProtocols = webSocketHandler.getSubProtocols();
		return subProtocols.toArray(new String[subProtocols.size()]);
	}

}
