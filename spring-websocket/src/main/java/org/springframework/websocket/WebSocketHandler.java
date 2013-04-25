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



/**
 * A handler for WebSocket sessions.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketHandler {

	/**
	 * A new WebSocket connection has been opened and is ready to be used.
	 */
	void afterConnectionEstablished(WebSocketSession session);

	/**
	 * A WebSocket connection has been closed.
	 */
	void afterConnectionClosed(CloseStatus closeStatus, WebSocketSession session);

	/**
	 * Handle an incoming text message.
	 */
	void handleTextMessage(TextMessage message, WebSocketSession session);

	/**
	 * Handle an incoming binary message.
	 */
	void handleBinaryMessage(BinaryMessage message, WebSocketSession session);

	/**
	 * TODO
	 */
	void handleTransportError(Throwable exception, WebSocketSession session);

}
