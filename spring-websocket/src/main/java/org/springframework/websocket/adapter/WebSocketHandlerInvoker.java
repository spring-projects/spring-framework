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
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;

/**
 * A class for managing and delegating to a {@link WebSocketHandler} instance, ensuring
 * the handler is initialized and destroyed, that any unhandled exceptions from handler
 * are caught (and handled by closing the session), as well as adding logging.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public class WebSocketHandlerInvoker implements WebSocketHandler {

	private Log logger = LogFactory.getLog(WebSocketHandlerInvoker.class);

	private final WebSocketHandler handler;

	private final AtomicInteger sessionCount = new AtomicInteger(0);


	public WebSocketHandlerInvoker(WebSocketHandler webSocketHandler) {
		Assert.notNull(webSocketHandler, "webSocketHandler is required");
		this.handler = webSocketHandler;
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

			this.handler.afterConnectionEstablished(session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex);
		}
	}

	public void tryCloseWithError(WebSocketSession session, Throwable ex) {
		tryCloseWithError(session, ex, null);
	}

	public void tryCloseWithError(WebSocketSession session, Throwable exeption, CloseStatus status) {
		logger.error("Closing due to exception for " + session, exeption);
		if (session.isOpen()) {
			try {
				session.close((status != null) ? status : CloseStatus.SERVER_ERROR);
			}
			catch (Throwable t) {
				// ignore
			}
		}
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received " + message + ", " + session);
		}
		try {
			this.handler.handleMessage(session, message);
		}
		catch (Throwable ex) {
			tryCloseWithError(session,ex);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Transport error for " + session, exception);
		}
		try {
			this.handler.handleTransportError(session, exception);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
		if (logger.isDebugEnabled()) {
			logger.debug("Connection closed for " + session + ", " + closeStatus);
		}
		try {
			this.handler.afterConnectionClosed(session, closeStatus);
		}
		catch (Throwable ex) {
			logger.error("Unhandled error for " + this, ex);
		}
	}

}
