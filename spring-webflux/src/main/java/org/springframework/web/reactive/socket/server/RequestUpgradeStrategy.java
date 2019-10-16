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

package org.springframework.web.reactive.socket.server;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A strategy for upgrading an HTTP request to a WebSocket session depending
 * on the underlying network runtime.
 *
 * <p>Typically there is one such strategy for every {@link ServerHttpRequest}
 * and {@link ServerHttpResponse} type except in the case of Servlet containers
 * for which the standard Java WebSocket API JSR-356 does not define a way to
 * upgrade a request so a custom strategy is needed for every Servlet container.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * Upgrade to a WebSocket session and handle it with the given handler.
	 * @param exchange the current exchange
	 * @param webSocketHandler handler for the WebSocket session
	 * @param subProtocol the selected sub-protocol got the handler
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 * @deprecated as of 5.1 in favor of
	 * {@link #upgrade(ServerWebExchange, WebSocketHandler, String, Supplier)}
	 */
	@Deprecated
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
			@Nullable String subProtocol) {

		return Mono.error(new UnsupportedOperationException());
	}

	/**
	 * Upgrade to a WebSocket session and handle it with the given handler.
	 * @param exchange the current exchange
	 * @param webSocketHandler handler for the WebSocket session
	 * @param subProtocol the selected sub-protocol got the handler
	 * @param handshakeInfoFactory factory to create HandshakeInfo for the WebSocket session
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 * @since 5.1
	 */
	@SuppressWarnings("deprecation")
	default Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		return upgrade(exchange, webSocketHandler, subProtocol);
	}

}
