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

package org.springframework.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;

/**
 * Allows sending messages over a WebSocket connection as well as closing it.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketSession {

	/**
	 * Return a unique session identifier.
	 */
	String getId();

	/**
	 * Return the URI used to open the WebSocket connection.
	 */
	URI getUri();

	/**
     * Return whether the underlying socket is using a secure transport.
	 */
	boolean isSecure();

	/**
	 * Return a {@link java.security.Principal} instance containing the name of the
	 * authenticated user. If the user has not been authenticated, the method returns
	 * <code>null</code>.
	 */
	Principal getPrincipal();

	/**
	 * Return the host name of the endpoint on the other end.
	 */
	String getRemoteHostName();

	/**
	 * Return the IP address of the endpoint on the other end.
	 */
	String getRemoteAddress();

	/**
	 * Return whether the connection is still open.
	 */
	boolean isOpen();

	/**
	 * Send a WebSocket message either {@link TextMessage} or
	 * {@link BinaryMessage}.
	 */
	void sendMessage(WebSocketMessage<?> message) throws IOException;

	/**
	 * Close the WebSocket connection with status 1000, i.e. equivalent to:
	 * <pre>
	 * session.close(CloseStatus.NORMAL);
	 * </pre>
	 */
	void close() throws IOException;

	/**
	 * Close the WebSocket connection with the given close status.
	 */
	void close(CloseStatus status) throws IOException;

}
