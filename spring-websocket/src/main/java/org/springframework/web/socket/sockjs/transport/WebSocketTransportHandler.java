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

import java.io.IOException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.sockjs.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.TransportErrorException;
import org.springframework.web.socket.sockjs.TransportHandler;
import org.springframework.web.socket.sockjs.TransportType;

/**
 * A WebSocket {@link TransportHandler}. Uses {@link SockJsWebSocketHandler} and
 * {@link WebSocketServerSockJsSession} to add SockJS processing.
 *
 * <p>Also implements {@link HandshakeHandler} to support raw WebSocket communication at
 * SockJS URL "/websocket".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketTransportHandler extends TransportHandlerSupport
		implements TransportHandler, SockJsSessionFactory, HandshakeHandler {

	private final HandshakeHandler handshakeHandler;


	public WebSocketTransportHandler(HandshakeHandler handshakeHandler) {
		Assert.notNull(handshakeHandler, "handshakeHandler must not be null");
		this.handshakeHandler = handshakeHandler;
	}


	@Override
	public TransportType getTransportType() {
		return TransportType.WEBSOCKET;
	}

	@Override
	public AbstractSockJsSession createSession(String sessionId, WebSocketHandler webSocketHandler) {
		return new WebSocketServerSockJsSession(sessionId, getSockJsConfig(), webSocketHandler);
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, AbstractSockJsSession session) throws TransportErrorException {

		try {
			WebSocketServerSockJsSession wsSession = (WebSocketServerSockJsSession) session;
			WebSocketHandler sockJsWrapper = new SockJsWebSocketHandler(getSockJsConfig(), webSocketHandler, wsSession);
			this.handshakeHandler.doHandshake(request, response, sockJsWrapper);
		}
		catch (Throwable t) {
			throw new TransportErrorException("Failed to start handshake request", t, session.getId());
		}
	}

	// HandshakeHandler methods

	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler)
			throws IOException {

		return this.handshakeHandler.doHandshake(request, response, handler);
	}

}
