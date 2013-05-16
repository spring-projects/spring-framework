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

package org.springframework.web.socket.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator;

/**
 * Adapts {@link WebSocketHandler} to the Jetty 9 {@link WebSocketListener}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class JettyWebSocketListenerAdapter implements WebSocketListener {

	private static final Log logger = LogFactory.getLog(JettyWebSocketListenerAdapter.class);

	private final WebSocketHandler webSocketHandler;

	private JettyWebSocketSessionAdapter wsSession;


	public JettyWebSocketListenerAdapter(WebSocketHandler webSocketHandler, JettyWebSocketSessionAdapter wsSession) {
		Assert.notNull(webSocketHandler, "webSocketHandler is required");
		Assert.notNull(wsSession, "wsSession is required");
		this.webSocketHandler = webSocketHandler;
		this.wsSession = wsSession;
	}


	@Override
	public void onWebSocketConnect(Session session) {
		this.wsSession.initSession(session);
		try {
			this.webSocketHandler.afterConnectionEstablished(this.wsSession);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

	@Override
	public void onWebSocketText(String payload) {
		TextMessage message = new TextMessage(payload);
		try {
			this.webSocketHandler.handleMessage(this.wsSession, message);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		BinaryMessage message = new BinaryMessage(payload, offset, len, true);
		try {
			this.webSocketHandler.handleMessage(this.wsSession, message);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		CloseStatus closeStatus = new CloseStatus(statusCode, reason);
		try {
			this.webSocketHandler.afterConnectionClosed(this.wsSession, closeStatus);
		}
		catch (Throwable t) {
			logger.error("Unhandled error for " + this.wsSession, t);
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		try {
			this.webSocketHandler.handleTransportError(this.wsSession, cause);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

}
