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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;

/**
 * Adapts Spring's {@link WebSocketHandler} to Jetty's {@link WebSocketListener}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class JettyWebSocketListenerAdapter implements WebSocketListener {

	private static Log logger = LogFactory.getLog(JettyWebSocketListenerAdapter.class);

	private final WebSocketHandler<WebSocketMessage<?>> webSocketHandler;

	private WebSocketSession wsSession;


	public JettyWebSocketListenerAdapter(WebSocketHandler<?> webSocketHandler) {
		Assert.notNull(webSocketHandler, "webSocketHandler is required");
		this.webSocketHandler = new WebSocketHandlerInvoker(webSocketHandler).setLogger(logger);
	}


	@Override
	public void onWebSocketConnect(Session session) {
		this.wsSession = new JettyWebSocketSessionAdapter(session);
		this.webSocketHandler.afterConnectionEstablished(this.wsSession);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		CloseStatus closeStatus = new CloseStatus(statusCode, reason);
		this.webSocketHandler.afterConnectionClosed(this.wsSession, closeStatus);
	}

	@Override
	public void onWebSocketText(String payload) {
		TextMessage message = new TextMessage(payload);
		this.webSocketHandler.handleMessage(this.wsSession, message);
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		BinaryMessage message = new BinaryMessage(payload, offset, len);
		this.webSocketHandler.handleMessage(this.wsSession, message);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		this.webSocketHandler.handleTransportError(this.wsSession, cause);
	}
}
