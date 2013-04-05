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
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.TransportType;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.StandardWebSocketServerSession;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.SockJsWebSocketHandler;
import org.springframework.websocket.server.HandshakeRequestHandler;
import org.springframework.websocket.server.endpoint.EndpointHandshakeRequestHandler;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketTransportHandler implements TransportHandler {


	@Override
	public TransportType getTransportType() {
		return TransportType.WEBSOCKET;
	}

	@Override
	public SockJsSessionSupport createSession(String sessionId, SockJsHandler handler, SockJsConfiguration config) {
		return new StandardWebSocketServerSession(sessionId, handler, config);
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response, SockJsSessionSupport session)
			throws Exception {

		StandardWebSocketServerSession sockJsSession = (StandardWebSocketServerSession) session;
		SockJsWebSocketHandler webSocketHandler = new SockJsWebSocketHandler(sockJsSession);
		HandshakeRequestHandler handshakeRequestHandler = new EndpointHandshakeRequestHandler(webSocketHandler);
		handshakeRequestHandler.doHandshake(request, response);
	}

}
