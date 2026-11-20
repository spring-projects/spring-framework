/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;

/**
 * Contract for initiating a WebSocket request. As an alternative considering using the
 * declarative style {@link WebSocketConnectionManager} that starts a WebSocket connection
 * to a pre-configured URI when the application starts.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see WebSocketConnectionManager
 */
public interface WebSocketClient {

	/**
	 * Execute a handshake request to the given url and handle the resulting
	 * WebSocket session with the given handler.
	 * @param webSocketHandler the session handler
	 * @param uriTemplate the url template
	 * @param uriVariables the variables to expand the template
	 * @return a future that completes when the session is available
	 * @since 6.0
	 */
	CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
				String uriTemplate, @Nullable Object... uriVariables);

	/**
	 * Execute a handshake request to the given url and handle the resulting
	 * WebSocket session with the given handler.
	 * @param webSocketHandler the session handler
	 * @param uri the url
	 * @return a future that completes when the session is available
	 * @since 6.0
	 */
	CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
			@Nullable WebSocketHttpHeaders headers, URI uri);

}
