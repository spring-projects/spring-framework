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

package org.springframework.web.socket.messaging;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;


/**
 * A contract for handling WebSocket messages as part of a higher level protocol, referred
 * to as "sub-protocol" in the WebSocket RFC specification. Handles both
 * {@link WebSocketMessage}s from a client as well as {@link Message}s to a client.
 * <p>
 * Implementations of this interface can be configured on a
 * {@link SubProtocolWebSocketHandler} which selects a sub-protocol handler to delegate
 * messages to based on the sub-protocol requested by the client through the
 * {@code Sec-WebSocket-Protocol} request header.
 *
 * @author Andy Wilkinson
 * @author Rossen Stoyanchev
 *
 * @since 4.0
 */
public interface SubProtocolHandler {

	/**
	 * Return the list of sub-protocols supported by this handler, never {@code null}.
	 */
	List<String> getSupportedProtocols();

	/**
	 * Handle the given {@link WebSocketMessage} received from a client.
	 *
	 * @param session the client session
	 * @param message the client message
	 * @param outputChannel an output channel to send messages to
	 */
	void handleMessageFromClient(WebSocketSession session, WebSocketMessage<?> message,
			MessageChannel outputChannel) throws Exception;

	/**
	 * Handle the given {@link Message} to the client associated with the given WebSocket
	 * session.
	 *
	 * @param session the client session
	 * @param message the client message
	 */
	void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception;

	/**
	 * Resolve the session id from the given message or return {@code null}.
	 *
	 * @param message the message to resolve the session id from
	 */
	String resolveSessionId(Message<?> message);

	/**
	 * Invoked after a {@link WebSocketSession} has started.
	 *
	 * @param session the client session
	 * @param outputChannel a channel
	 */
	void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception;

	/**
	 * Invoked after a {@link WebSocketSession} has ended.
	 *
	 * @param session the client session
	 * @param closeStatus the reason why the session was closed
	 * @param outputChannel a channel
	 */
	void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus,
			MessageChannel outputChannel) throws Exception;

}
