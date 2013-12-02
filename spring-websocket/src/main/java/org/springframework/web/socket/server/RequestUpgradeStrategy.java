/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;

/**
 * A server-specific strategy for performing the actual upgrade to a WebSocket exchange.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * Return the supported WebSocket protocol versions.
	 */
	String[] getSupportedVersions();

	/**
	 * @return the WebSocket protocol extensions supported by the underlying WebSocket server.
	 */
	List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request);

	/**
	 * Perform runtime specific steps to complete the upgrade. Invoked after successful
	 * negotiation of the handshake request.
	 * @param request the current request
	 * @param response the current response
	 * @param selectedProtocol the selected sub-protocol, if any
	 * @param selectedExtensions the selected WebSocket protocol extensions
	 * @param wsHandler the handler for WebSocket messages
	 * @param attributes handshake request specific attributes to be set on the WebSocket
	 * session via {@link org.springframework.web.socket.server.HandshakeInterceptor}
	 * and thus made available to the
	 * {@link org.springframework.web.socket.WebSocketHandler};
	 * @throws HandshakeFailureException thrown when handshake processing failed to
	 * complete due to an internal, unrecoverable error, i.e. a server error as
	 * opposed to a failure to successfully negotiate the requirements of the
	 * handshake request.
	 */
	void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException;

}
