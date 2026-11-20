/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;

/**
 * A server-specific strategy for performing the actual upgrade to a WebSocket exchange.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy
 */
public interface RequestUpgradeStrategy {

	/**
	 * Return the supported WebSocket protocol versions.
	 */
	String[] getSupportedVersions();

	/**
	 * Return the WebSocket protocol extensions supported by the underlying WebSocket server.
	 */
	List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request);

	/**
	 * Perform runtime specific steps to complete the upgrade. Invoked after successful
	 * negotiation of the handshake request.
	 * @param request the current request
	 * @param response the current response
	 * @param selectedProtocol the selected sub-protocol, if any
	 * @param selectedExtensions the selected WebSocket protocol extensions
	 * @param user the user to associate with the WebSocket session
	 * @param wsHandler the handler for WebSocket messages
	 * @param attributes handshake request specific attributes to be set on the WebSocket
	 * session via {@link org.springframework.web.socket.server.HandshakeInterceptor} and
	 * thus made available to the {@link org.springframework.web.socket.WebSocketHandler}
	 * @throws HandshakeFailureException thrown when handshake processing failed to
	 * complete due to an internal, unrecoverable error, i.e. a server error as
	 * opposed to a failure to successfully negotiate the requirements of the
	 * handshake request.
	 */
	void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			@Nullable Principal user, WebSocketHandler wsHandler, Map<String, Object> attributes)
			throws HandshakeFailureException;

}
