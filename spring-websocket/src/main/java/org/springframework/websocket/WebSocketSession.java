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
import java.net.URI;
import java.nio.ByteBuffer;



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
	 * Return whether the connection is still open.
	 */
	boolean isOpen();

	/**
     * Return whether the underlying socket is using a secure transport.
	 */
	boolean isSecure();

	/**
	 * Return the URI used to open the WebSocket connection.
	 */
	URI getURI();

	/**
	 * Send a text message.
	 */
	void sendTextMessage(String message) throws IOException;

	/**
	 * Send a binary message.
	 */
	void sendBinaryMessage(ByteBuffer message) throws IOException;

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
