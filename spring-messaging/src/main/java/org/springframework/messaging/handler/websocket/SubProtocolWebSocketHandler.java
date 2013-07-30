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

package org.springframework.messaging.handler.websocket;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;


/**
 * A {@link WebSocketHandler} that delegates messages to a {@link SubProtocolHandler}
 * based on the sub-protocol value requested by the client through the
 * {@code Sec-WebSocket-Protocol} request header A default handler can also be configured
 * to use if the client does not request a specific sub-protocol.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 *
 * @since 4.0
 */
public class SubProtocolWebSocketHandler implements WebSocketHandler, MessageHandler {

	private final Log logger = LogFactory.getLog(SubProtocolWebSocketHandler.class);

	private final MessageChannel outputChannel;

	private final Map<String, SubProtocolHandler> protocolHandlers =
			new TreeMap<String, SubProtocolHandler>(String.CASE_INSENSITIVE_ORDER);

	private SubProtocolHandler defaultProtocolHandler;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();


	/**
	 * @param outputChannel
	 */
	public SubProtocolWebSocketHandler(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel is required");
		this.outputChannel = outputChannel;
	}

	/**
	 * Configure one or more handlers to use depending on the sub-protocol requested by
	 * the client in the WebSocket handshake request.
	 *
	 * @param protocolHandlers the sub-protocol handlers to use
	 */
	public void setProtocolHandlers(List<SubProtocolHandler> protocolHandlers) {
		this.protocolHandlers.clear();
		for (SubProtocolHandler handler: protocolHandlers) {
			List<String> protocols = handler.getSupportedProtocols();
			if (CollectionUtils.isEmpty(protocols)) {
				logger.warn("No sub-protocols, ignoring handler " + handler);
				continue;
			}
			for (String protocol: protocols) {
				SubProtocolHandler replaced = this.protocolHandlers.put(protocol, handler);
				if (replaced != null) {
					throw new IllegalStateException("Failed to map handler " + handler
							+ " to protocol '" + protocol + "', it is already mapped to handler " + replaced);
				}
			}
		}
		if ((this.protocolHandlers.size() == 1) &&(this.defaultProtocolHandler == null)) {
			this.defaultProtocolHandler = this.protocolHandlers.values().iterator().next();
		}
	}

	/**
	 * @return the configured sub-protocol handlers
	 */
	public Map<String, SubProtocolHandler> getProtocolHandlers() {
		return this.protocolHandlers;
	}

	/**
	 * Set the {@link SubProtocolHandler} to use when the client did not request a
	 * sub-protocol.
	 *
	 * @param defaultProtocolHandler the default handler
	 */
	public void setDefaultProtocolHandler(SubProtocolHandler defaultProtocolHandler) {
		this.defaultProtocolHandler = defaultProtocolHandler;
		if (this.protocolHandlers.isEmpty()) {
			setProtocolHandlers(Arrays.asList(defaultProtocolHandler));
		}
	}

	/**
	 * @return the default sub-protocol handler to use
	 */
	public SubProtocolHandler getDefaultProtocolHandler() {
		return this.defaultProtocolHandler;
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.sessions.put(session.getId(), session);
		getProtocolHandler(session).afterSessionStarted(session, this.outputChannel);
	}

	protected final SubProtocolHandler getProtocolHandler(WebSocketSession session) {
		SubProtocolHandler handler;
		String protocol = session.getAcceptedProtocol();
		if (protocol != null) {
			handler = this.protocolHandlers.get(protocol);
			Assert.state(handler != null,
					"No handler for sub-protocol '" + protocol + "', handlers=" + this.protocolHandlers);
		}
		else {
			handler = this.defaultProtocolHandler;
			Assert.state(handler != null,
					"No sub-protocol was requested and a default sub-protocol handler was not configured");
		}
		return handler;
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		getProtocolHandler(session).handleMessageFromClient(session, message, this.outputChannel);
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		String sessionId = resolveSessionId(message);
		if (sessionId == null) {
			logger.error("sessionId not found in message " + message);
			return;
		}

		WebSocketSession session = this.sessions.get(sessionId);
		if (session == null) {
			logger.error("Session not found for session with id " + sessionId);
			return;
		}

		try {
			getProtocolHandler(session).handleMessageToClient(session, message);
		}
		catch (Exception e) {
			logger.error("Failed to send message to client " + message, e);
		}
	}

	private String resolveSessionId(Message<?> message) {
		for (SubProtocolHandler handler : this.protocolHandlers.values()) {
			String sessionId = handler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		if (this.defaultProtocolHandler != null) {
			String sessionId = this.defaultProtocolHandler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		return null;
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		this.sessions.remove(session.getId());
		getProtocolHandler(session).afterSessionEnded(session, closeStatus, this.outputChannel);
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

}
