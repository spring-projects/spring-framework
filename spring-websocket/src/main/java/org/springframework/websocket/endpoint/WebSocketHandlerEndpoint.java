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

import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.PartialMessageHandler;
import org.springframework.websocket.TextMessage;
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

	private WebSocketHandler handler;

	private WebSocketSession webSocketSession;

	private final AtomicInteger sessionCount = new AtomicInteger(0);


	public WebSocketHandlerEndpoint(HandlerProvider<WebSocketHandler> handlerProvider) {
		Assert.notNull(handlerProvider, "handlerProvider is required");
		this.handlerProvider = handlerProvider;
	}


	@Override
	public void onOpen(final javax.websocket.Session session, EndpointConfig config) {

		Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected connection");

		if (logger.isDebugEnabled()) {
			logger.debug("Connection established, javax.websocket.Session id="
					+ session.getId() + ", uri=" + session.getRequestURI());
		}

		this.webSocketSession = new StandardWebSocketSession(session);
		this.handler = handlerProvider.getHandler();

		session.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				handleTextMessage(session, message);
			}
		});
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

		try {
			this.handler.afterConnectionEstablished(this.webSocketSession);
		}
		catch (Throwable ex) {
			tryCloseWithError(ex);
		}
	}

	private void tryCloseWithError(Throwable ex) {
		logger.error("Unhandled error for " + this.webSocketSession, ex);
		if (this.webSocketSession.isOpen()) {
			try {
				this.webSocketSession.close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable t) {
				destroyHandler();
			}
		}
	}

	private void destroyHandler() {
		try {
			if (this.handler != null) {
				this.handlerProvider.destroy(this.handler);
			}
		}
		catch (Throwable t) {
			logger.warn("Error while destroying handler", t);
		}
		finally {
			this.webSocketSession = null;
			this.handler = null;
		}
	}

	private void handleTextMessage(javax.websocket.Session session, String message) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received message for WebSocket session id=" + session.getId() + ": " + message);
		}
		try {
			TextMessage textMessage = new TextMessage(message);
			this.handler.handleTextMessage(textMessage, this.webSocketSession);
		}
		catch (Throwable ex) {
			tryCloseWithError(ex);
		}
	}

	private void handleBinaryMessage(javax.websocket.Session session, byte[] message, boolean isLast) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received binary data for WebSocket session id=" + session.getId());
		}
		try {
			BinaryMessage binaryMessage = new BinaryMessage(message, isLast);
			this.handler.handleBinaryMessage(binaryMessage, this.webSocketSession);
		}
		catch (Throwable ex) {
			tryCloseWithError(ex);
		}
	}

	@Override
	public void onClose(javax.websocket.Session session, CloseReason reason) {
		if (logger.isDebugEnabled()) {
			logger.debug("Connection closed, WebSocket session id=" + session.getId() + ", " + reason);
		}
		try {
			CloseStatus closeStatus = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
			this.handler.afterConnectionClosed(closeStatus, this.webSocketSession);
		}
		catch (Throwable ex) {
			logger.error("Unhandled error for " + this.webSocketSession, ex);
		}
		finally {
			this.handlerProvider.destroy(this.handler);
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable exception) {
		logger.error("Error for WebSocket session id=" + session.getId(), exception);
		try {
			this.handler.handleTransportError(exception, this.webSocketSession);
		}
		catch (Throwable ex) {
			tryCloseWithError(ex);
		}
	}

}
