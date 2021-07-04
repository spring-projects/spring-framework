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

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A service to delegate WebSocket-related HTTP requests to.
 *
 * <p>For a WebSocket endpoint this means handling the initial WebSocket HTTP
 * handshake request. For a SockJS endpoint it could mean handling all HTTP
 * requests defined in the SockJS protocol.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
 */
public interface WebSocketService {

	/**
	 * Handle the request with the given {@link WebSocketHandler}.
	 * @param exchange the current exchange
	 * @param webSocketHandler handler for WebSocket session
	 * @return a {@code Mono<Void>} that completes when application handling of
	 * the WebSocket session completes.
	 */
	Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler);

}
