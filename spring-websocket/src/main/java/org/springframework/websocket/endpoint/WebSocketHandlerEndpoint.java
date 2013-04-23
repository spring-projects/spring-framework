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

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.BinaryMessageHandler;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.PartialMessageHandler;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.TextMessageHandler;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;


/**
 * An {@link Endpoint} that delegates to a {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerEndpoint extends Endpoint {

	private static Log logger = LogFactory.getLog(WebSocketHandlerEndpoint.class);

	private final HandlerProvider<WebSocketHandler> handlerProvider;

	private final WebSocketHandler handler;

	private WebSocketSession webSocketSession;


	public WebSocketHandlerEndpoint(HandlerProvider<WebSocketHandler> handlerProvider) {
		Assert.notNull(handlerProvider, "handlerProvider is required");
		this.handlerProvider = handlerProvider;
		this.handler = handlerProvider.getHandler();
	}

	@Override
	public void onOpen(final javax.websocket.Session session, EndpointConfig config) {
		if (logger.isDebugEnabled()) {
			logger.debug("Client connected, WebSocket session id=" + session.getId() + ", uri=" + session.getRequestURI());
		}
		try {
			this.webSocketSession = new StandardWebSocketSession(session);

			if (this.handler instanceof TextMessageHandler) {
				session.addMessageHandler(new MessageHandler.Whole<String>() {
					@Override
					public void onMessage(String message) {
						handleTextMessage(session, message);
					}
				});
			}
			else if (this.handler instanceof BinaryMessageHandler) {
				if (this.handler instanceof PartialMessageHandler) {
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
				logger.warn("WebSocketHandler handles neither text nor binary messages: " + this.handler);
			}

			this.handler.afterConnectionEstablished(this.webSocketSession);
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
		try {
			TextMessage textMessage = new TextMessage(message);
			((TextMessageHandler) handler).handleTextMessage(textMessage, this.webSocketSession);
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
		try {
			BinaryMessage binaryMessage = new BinaryMessage(message, isLast);
			((BinaryMessageHandler) handler).handleBinaryMessage(binaryMessage, this.webSocketSession);
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
			CloseStatus closeStatus = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
			this.handler.afterConnectionClosed(closeStatus, this.webSocketSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing session closing", ex);
		}
		finally {
			this.handlerProvider.destroy(this.handler);
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable exception) {
		logger.error("Error for WebSocket session id=" + session.getId(), exception);
		try {
			this.handler.handleError(exception, this.webSocketSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Failed to handle error", ex);
		}
	}

}
