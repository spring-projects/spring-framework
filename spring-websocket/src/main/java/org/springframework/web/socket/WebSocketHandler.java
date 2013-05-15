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

package org.springframework.web.socket;

/**
 * A handler for WebSocket messages and lifecycle events.
 *
 * <p>
 * Implementations of this interface are encouraged to handle exceptions locally where it
 * makes sense or alternatively let the exception bubble up in which case the exception is
 * logged and the session closed with {@link CloseStatus#SERVER_ERROR SERVER_ERROR(1011)}
 * by default. The exception handling strategy is provided by
 * {@link org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator
 * ExceptionWebSocketHandlerDecorator}, which can be customized or replaced by decorating
 * the {@link WebSocketHandler} with a different decorator.
 *
 * @param <T> The type of message being handled {@link TextMessage}, {@link BinaryMessage}
 *        (or {@link WebSocketMessage} for both).
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public interface WebSocketHandler {

	/**
	 * Invoked after WebSocket negotiation has succeeded and the WebSocket connection is
	 * opened and ready for use.
	 *
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 *         Javadoc for details.
	 */
	void afterConnectionEstablished(WebSocketSession session) throws Exception;

	/**
	 * Invoked when a new WebSocket message arrives.
	 *
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 *         Javadoc for details.
	 */
	void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception;

	/**
	 * Handle an error from the underlying WebSocket message transport.
	 *
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 *         Javadoc for details.
	 */
	void handleTransportError(WebSocketSession session, Throwable exception) throws Exception;

	/**
	 * Invoked after the WebSocket connection has been closed by either side, or after a
	 * transport error has occurred. Although the session may technically still be open,
	 * depending on the underlying implementation, sending messages at this point is
	 * discouraged and most likely will not succeed.
	 *
	 * @throws Exception this method can handle or propagate exceptions; see class-level
	 *         Javadoc for details.
	 */
	void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception;

}
