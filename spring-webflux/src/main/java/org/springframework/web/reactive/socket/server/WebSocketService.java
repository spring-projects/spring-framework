/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
