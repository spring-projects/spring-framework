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

package org.springframework.websocket.adapter;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

/**
 * A class for managing and delegating to a {@link WebSocketHandler} instance, applying
 * initialization and destruction as necessary at the start and end of the WebSocket
 * session, ensuring that any unhandled exceptions from its methods are caught and handled
 * by closing the session, and also adding uniform logging.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerInvoker implements WebSocketHandler {

	private Log logger = LogFactory.getLog(WebSocketHandlerInvoker.class);

	private final HandlerProvider<WebSocketHandler> handlerProvider;

	private WebSocketHandler handler;

	private final AtomicInteger sessionCount = new AtomicInteger(0);


	public WebSocketHandlerInvoker(HandlerProvider<WebSocketHandler> handlerProvider) {
		this.handlerProvider = handlerProvider;
	}

	public WebSocketHandlerInvoker setLogger(Log logger) {
		this.logger = logger;
		return this;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		if (logger.isDebugEnabled()) {
			logger.debug("Connection established, "	+ session + ", uri=" + session.getURI());
		}
		try {
			Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected new session");

			this.handler = this.handlerProvider.getHandler();
			this.handler.afterConnectionEstablished(session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex);
		}
	}

	public void tryCloseWithError(WebSocketSession session, Throwable ex) {
		tryCloseWithError(session, ex, null);
	}

	public void tryCloseWithError(WebSocketSession session, Throwable ex, CloseStatus status) {
		logger.error("Unhandled error for " + session, ex);
		if (session.isOpen()) {
			try {
				session.close(CloseStatus.SERVER_ERROR);
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
			this.handler = null;
		}
	}

	@Override
	public void handleTextMessage(TextMessage message, WebSocketSession session) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received text message for " + session + ": " + message);
		}
		try {
			this.handler.handleTextMessage(message, session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session,ex);
		}
	}

	@Override
	public void handleBinaryMessage(BinaryMessage message, WebSocketSession session) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received binary message for " + session);
		}
		try {
			this.handler.handleBinaryMessage(message, session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex);
		}
	}

	@Override
	public void handleTransportError(Throwable exception, WebSocketSession session) {
		if (logger.isDebugEnabled()) {
			logger.debug("Transport error for " + session, exception);
		}
		try {
			this.handler.handleTransportError(exception, session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex);
		}
	}

	@Override
	public void afterConnectionClosed(CloseStatus closeStatus, WebSocketSession session) {
		if (logger.isDebugEnabled()) {
			logger.debug("Connection closed for " + session + ", " + closeStatus);
		}
		try {
			this.handler.afterConnectionClosed(closeStatus, session);
		}
		catch (Throwable ex) {
			logger.error("Unhandled error for " + this, ex);
		}
		finally {
			this.handlerProvider.destroy(this.handler);
		}
	}

}
