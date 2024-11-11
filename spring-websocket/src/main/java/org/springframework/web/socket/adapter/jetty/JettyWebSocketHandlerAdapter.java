/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.adapter.jetty;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;

/**
 * Adapts {@link WebSocketHandler} to the Jetty WebSocket API {@link org.eclipse.jetty.websocket.api.Session.Listener}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketHandlerAdapter implements Session.Listener {

	private static final Log logger = LogFactory.getLog(JettyWebSocketHandlerAdapter.class);

	private final WebSocketHandler webSocketHandler;

	private final JettyWebSocketSession wsSession;

	@Nullable
	private Session nativeSession;


	public JettyWebSocketHandlerAdapter(WebSocketHandler webSocketHandler, JettyWebSocketSession wsSession) {
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		Assert.notNull(wsSession, "WebSocketSession must not be null");
		this.webSocketHandler = webSocketHandler;
		this.wsSession = wsSession;
	}

	@Override
	public void onWebSocketOpen(Session session) {
		try {
			this.nativeSession = session;
			this.wsSession.initializeNativeSession(session);
			this.webSocketHandler.afterConnectionEstablished(this.wsSession);
			this.nativeSession.demand();
		}
		catch (Exception ex) {
			tryCloseWithError(ex);
		}
	}

	@Override
	public void onWebSocketText(String payload) {
		Assert.state(this.nativeSession != null, "No native session available");
		TextMessage message = new TextMessage(payload);
		try {
			this.webSocketHandler.handleMessage(this.wsSession, message);
			this.nativeSession.demand();
		}
		catch (Exception ex) {
			tryCloseWithError(ex);
		}
	}

	@Override
	public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
		Assert.state(this.nativeSession != null, "No native session available");
		BinaryMessage message = new BinaryMessage(BufferUtil.copy(payload), true);
		callback.succeed();
		try {
			this.webSocketHandler.handleMessage(this.wsSession, message);
			this.nativeSession.demand();
		}
		catch (Exception ex) {
			tryCloseWithError(ex);
		}
	}

	@Override
	public void onWebSocketPong(ByteBuffer payload) {
		Assert.state(this.nativeSession != null, "No native session available");
		PongMessage message = new PongMessage(BufferUtil.copy(payload));
		try {
			this.webSocketHandler.handleMessage(this.wsSession, message);
			this.nativeSession.demand();
		}
		catch (Exception ex) {
			tryCloseWithError(ex);
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		CloseStatus closeStatus = new CloseStatus(statusCode, reason);
		try {
			this.webSocketHandler.afterConnectionClosed(this.wsSession, closeStatus);
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unhandled exception from afterConnectionClosed for " + this, ex);
			}
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		try {
			this.webSocketHandler.handleTransportError(this.wsSession, cause);
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unhandled exception from handleTransportError for " + this, ex);
			}
		}
	}

	private void tryCloseWithError(Throwable t) {
		if (this.nativeSession != null) {
			if (this.nativeSession.isOpen()) {
				ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
			}
			else {
				// Session might be O-SHUT waiting for response close frame, so abort to close the connection.
				this.nativeSession.disconnect();
			}
		}
	}
}
