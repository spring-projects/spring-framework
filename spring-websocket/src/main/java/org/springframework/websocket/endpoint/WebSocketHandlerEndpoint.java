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

package org.springframework.websocket.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.BinaryMessageHandler;
import org.springframework.websocket.PartialMessageHandler;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.TextMessageHandler;


/**
 * An {@link Endpoint} that delegates to a {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerEndpoint extends Endpoint {

	private static Log logger = LogFactory.getLog(WebSocketHandlerEndpoint.class);

	private final WebSocketHandler webSocketHandler;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();


	public WebSocketHandlerEndpoint(WebSocketHandler handler) {
		Assert.notNull(handler, "webSocketHandler is required");
		this.webSocketHandler = handler;
	}

	@Override
	public void onOpen(final javax.websocket.Session session, EndpointConfig config) {
		if (logger.isDebugEnabled()) {
			logger.debug("Client connected, WebSocket session id=" + session.getId() + ", uri=" + session.getRequestURI());
		}
		try {
			WebSocketSession webSocketSession = new StandardWebSocketSession(session);
			this.sessions.put(session.getId(), webSocketSession);

			if (this.webSocketHandler instanceof TextMessageHandler) {
				session.addMessageHandler(new MessageHandler.Whole<String>() {
					@Override
					public void onMessage(String message) {
						handleTextMessage(session, message);
					}
				});
			}
			else if (this.webSocketHandler instanceof BinaryMessageHandler) {
				if (this.webSocketHandler instanceof PartialMessageHandler) {
					session.addMessageHandler(new MessageHandler.Partial<byte[]>() {
						@Override
						public void onMessage(byte[] messagePart, boolean isLast) {
							handleBinaryMessage(session, messagePart, isLast);
						}
					});
				}
				else {
					session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
						@Override
						public void onMessage(byte[] message) {
							handleBinaryMessage(session, message, true);
						}
					});
				}
			}
			else {
				logger.warn("WebSocketHandler handles neither text nor binary messages: " + this.webSocketHandler);
			}

			this.webSocketHandler.afterConnectionEstablished(webSocketSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing new session", ex);
		}
	}

	private void handleTextMessage(javax.websocket.Session session, String message) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received message for WebSocket session id=" + session.getId() + ": " + message);
		}
		WebSocketSession wsSession = getWebSocketSession(session);
		Assert.notNull(wsSession, "WebSocketSession not found");
		try {
			TextMessage textMessage = new TextMessage(message);
			((TextMessageHandler) webSocketHandler).handleTextMessage(textMessage, wsSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing message", ex);
		}
	}

	private void handleBinaryMessage(javax.websocket.Session session, byte[] message, boolean isLast) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received binary data for WebSocket session id=" + session.getId());
		}
		WebSocketSession wsSession = getWebSocketSession(session);
		Assert.notNull(wsSession, "WebSocketSession not found");
		try {
			BinaryMessage binaryMessage = new BinaryMessage(message, isLast);
			((BinaryMessageHandler) webSocketHandler).handleBinaryMessage(binaryMessage, wsSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing message", ex);
		}
	}

	@Override
	public void onClose(javax.websocket.Session session, CloseReason reason) {
		if (logger.isDebugEnabled()) {
			logger.debug("Client disconnected, WebSocket session id=" + session.getId() + ", " + reason);
		}
		try {
			WebSocketSession wsSession = this.sessions.remove(session.getId());
			if (wsSession != null) {
				CloseStatus closeStatus = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
				this.webSocketHandler.afterConnectionClosed(closeStatus, wsSession);
			}
			else {
				Assert.notNull(wsSession, "No WebSocket session");
			}
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing session closing", ex);
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable exception) {
		logger.error("Error for WebSocket session id=" + session.getId(), exception);
		try {
			WebSocketSession wsSession = getWebSocketSession(session);
			if (wsSession != null) {
				this.webSocketHandler.handleError(exception, wsSession);
			}
			else {
				logger.warn("WebSocketSession not found. Perhaps onError was called after onClose?");
			}
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Failed to handle error", ex);
		}
	}

	private WebSocketSession getWebSocketSession(javax.websocket.Session session) {
		return this.sessions.get(session.getId());
	}

}
