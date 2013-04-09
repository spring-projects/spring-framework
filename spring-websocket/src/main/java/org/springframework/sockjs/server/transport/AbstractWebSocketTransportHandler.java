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

package org.springframework.sockjs.server.transport;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportType;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.server.HandshakeHandler;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketTransportHandler implements TransportHandler, HandshakeHandler {

	private final HandshakeHandler sockJsHandshakeHandler;

	private final HandshakeHandler handshakeHandler;


	public AbstractWebSocketTransportHandler(SockJsConfiguration sockJsConfig) {
		this.sockJsHandshakeHandler = createHandshakeHandler(new SockJsWebSocketHandler(sockJsConfig));
		this.handshakeHandler = createHandshakeHandler(new WebSocketSockJsHandlerAdapter(sockJsConfig));
	}

	protected abstract HandshakeHandler createHandshakeHandler(WebSocketHandler webSocketHandler);

	@Override
	public TransportType getTransportType() {
		return TransportType.WEBSOCKET;
	}

	@Override
	public boolean canCreateSession() {
		return false;
	}

	@Override
	public SockJsSessionSupport createSession(String sessionId) {
		throw new IllegalStateException("WebSocket transport handlers do not create new sessions");
	}

	@Override
	public boolean handleNoSession(ServerHttpRequest request, ServerHttpResponse response) {
		return true;
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsSessionSupport session) throws Exception {

		this.sockJsHandshakeHandler.doHandshake(request, response);
	}

	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response) throws Exception {
		return this.handshakeHandler.doHandshake(request, response);
	}

}
