/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A strategy for upgrading an HTTP request to a WebSocket session depending
 * on the underlying network runtime.
 *
 * <p>Typically there is one such strategy for every {@link ServerHttpRequest}
 * and {@link ServerHttpResponse} type except in the case of Servlet containers
 * for which the standard Jakarta WebSocket API (JSR-356) does not define a way to
 * upgrade a request so a custom strategy is needed for every Servlet container.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see org.springframework.web.reactive.socket.server.upgrade.StandardWebSocketUpgradeStrategy
 */
public interface RequestUpgradeStrategy {

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
	Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory);

}
