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

package org.springframework.web.socket.sockjs.transport;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.sockjs.SockJsConfiguration;


/**
 * A wrapper around a {@link WebSocketHandler} instance that parses and adds SockJS
 * messages frames and also sends SockJS heartbeat messages.
 *
 * <p>
 * Implementations of the {@link WebSocketHandler} interface in this class allow
 * exceptions from the wrapped {@link WebSocketHandler} to propagate. However, any
 * exceptions resulting from SockJS message handling (e.g. while sending SockJS frames or
 * heartbeat messages) are caught and treated as transport errors, i.e. routed to the
 * {@link WebSocketHandler#handleTransportError(WebSocketSession, Throwable)
 * handleTransportError} method of the wrapped handler and the session closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler extends TextWebSocketHandlerAdapter {

	private final SockJsConfiguration sockJsConfig;

	private WebSocketServerSockJsSession session;

	private final AtomicInteger sessionCount = new AtomicInteger(0);


	public SockJsWebSocketHandler(SockJsConfiguration config,
			WebSocketHandler webSocketHandler, WebSocketServerSockJsSession session) {

		Assert.notNull(config, "sockJsConfig is required");
		Assert.notNull(webSocketHandler, "webSocketHandler is required");
		Assert.notNull(session, "session is required");

		this.sockJsConfig = config;
		this.session = session;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
		Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected connection");
		this.session.initWebSocketSession(wsSession);
	}

	@Override
	public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
		this.session.handleMessage(message, wsSession);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
		this.session.delegateConnectionClosed(status);
	}

	@Override
	public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) throws Exception {
		this.session.delegateError(exception);
	}

}
