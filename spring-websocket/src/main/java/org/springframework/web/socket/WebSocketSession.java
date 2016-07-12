/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * A WebSocket session abstraction. Allows sending messages over a WebSocket
 * connection and closing it.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketSession extends Closeable {

	/**
	 * Return a unique session identifier.
	 */
	String getId();

	/**
	 * Return the URI used to open the WebSocket connection.
	 */
	URI getUri();

	/**
	 * Return the headers used in the handshake request.
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * Return the map with attributes associated with the WebSocket session.
	 * <p>On the server side the map can be populated initially through a
	 * {@link org.springframework.web.socket.server.HandshakeInterceptor
	 * HandshakeInterceptor}. On the client side the map can be populated via
	 * {@link org.springframework.web.socket.client.WebSocketClient
	 * WebSocketClient} handshake methods.
	 * @return a Map with the session attributes, never {@code null}.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Return a {@link java.security.Principal} instance containing the name of the
	 * authenticated user.
	 * <p>If the user has not been authenticated, the method returns <code>null</code>.
	 */
	Principal getPrincipal();

	/**
	 * Return the address on which the request was received.
	 */
	InetSocketAddress getLocalAddress();

	/**
	 * Return the address of the remote client.
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * Return the negotiated sub-protocol or {@code null} if none was specified or
	 * negotiated successfully.
	 */
	String getAcceptedProtocol();

	/**
	 * Configure the maximum size for an incoming text message.
	 */
	default void setTextMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	/**
	 * Get the configured maximum size for an incoming text message.
	 */
	default int getTextMessageSizeLimit() {
		return -1;
	}

	/**
	 * Configure the maximum size for an incoming binary message.
	 */
	default void setBinaryMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	/**
	 * Get the configured maximum size for an incoming binary message.
	 */
	default int getBinaryMessageSizeLimit() {
		return -1;
	}

	/**
	 * Return the negotiated extensions or {@code null} if none was specified or
	 * negotiated successfully.
	 */
	List<WebSocketExtension> getExtensions();

	/**
	 * Send a WebSocket message: either {@link TextMessage} or {@link BinaryMessage}.
	 */
	void sendMessage(WebSocketMessage<?> message) throws IOException;

	/**
	 * Return whether the connection is still open.
	 */
	boolean isOpen();

	/**
	 * Close the WebSocket connection with status 1000, i.e. equivalent to:
	 * <pre class="code">
	 * session.close(CloseStatus.NORMAL);
	 * </pre>
	 */
	@Override
	void close() throws IOException;

	/**
	 * Close the WebSocket connection with the given close status.
	 */
	void close(CloseStatus status) throws IOException;

}
