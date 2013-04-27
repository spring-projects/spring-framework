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

package org.springframework.websocket.server;

import java.io.IOException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.websocket.WebSocketHandler;

/**
 * A strategy for performing container-specific steps to upgrade an HTTP request during a
 * WebSocket handshake. Intended for use within {@link HandshakeHandler} implementations.
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
	 * Perform runtime specific steps to complete the upgrade.
	 * Invoked only if the handshake is successful.
	 *
	 * @param handler the handler for WebSocket messages
	 */
	void upgrade(ServerHttpRequest request, ServerHttpResponse response, String selectedProtocol,
			WebSocketHandler<?> webSocketHandler) throws IOException;

}
