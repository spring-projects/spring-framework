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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.ConfigurableTransportHandler;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportType;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.server.HandshakeHandler;


/**
 * A WebSocket {@link TransportHandler} that delegates to a {@link HandshakeHandler}
 * passing a SockJS {@link WebSocketHandler}. Also implements {@link HandshakeHandler}
 * directly in support for raw WebSocket communication at SockJS URL "/websocket".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketTransportHandler implements ConfigurableTransportHandler, HandshakeHandler {

	private final HandshakeHandler handshakeHandler;

	private SockJsConfiguration sockJsConfig;

	private final Map<SockJsHandler, WebSocketHandler> sockJsHandlers = new HashMap<SockJsHandler, WebSocketHandler>();

	private final Collection<WebSocketHandler> rawWebSocketHandlers = new ArrayList<WebSocketHandler>();


	public WebSocketTransportHandler(HandshakeHandler handshakeHandler) {
		Assert.notNull(handshakeHandler, "handshakeHandler is required");
		this.handshakeHandler = handshakeHandler;
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.WEBSOCKET;
	}

	@Override
	public void setSockJsConfiguration(SockJsConfiguration sockJsConfig) {
		this.sockJsConfig = sockJsConfig;
	}

	@Override
	public void registerSockJsHandlers(Collection<SockJsHandler> sockJsHandlers) {
		this.sockJsHandlers.clear();
		for (SockJsHandler sockJsHandler : sockJsHandlers) {
			this.sockJsHandlers.put(sockJsHandler, adaptSockJsHandler(sockJsHandler));
		}
		this.handshakeHandler.registerWebSocketHandlers(getAllWebSocketHandlers());
	}

	/**
	 * Adapt the {@link SockJsHandler} to the {@link WebSocketHandler} contract for
	 * exchanging SockJS message over WebSocket.
	 */
	protected WebSocketHandler adaptSockJsHandler(SockJsHandler sockJsHandler) {
		return new SockJsWebSocketHandler(this.sockJsConfig, sockJsHandler);
	}

	private Collection<WebSocketHandler> getAllWebSocketHandlers() {
		Set<WebSocketHandler> handlers = new HashSet<WebSocketHandler>();
		handlers.addAll(this.sockJsHandlers.values());
		handlers.addAll(this.rawWebSocketHandlers);
		return handlers;
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsHandler sockJsHandler, SockJsSessionSupport session) throws Exception {

		WebSocketHandler webSocketHandler = this.sockJsHandlers.get(sockJsHandler);
		if (webSocketHandler == null) {
			webSocketHandler = adaptSockJsHandler(sockJsHandler);
		}

		this.handshakeHandler.doHandshake(request, response, webSocketHandler);
	}

	// HandshakeHandler methods

	@Override
	public void registerWebSocketHandlers(Collection<WebSocketHandler> webSocketHandlers) {
		this.rawWebSocketHandlers.clear();
		this.rawWebSocketHandlers.addAll(webSocketHandlers);
		this.handshakeHandler.registerWebSocketHandlers(getAllWebSocketHandlers());
	}

	@Override
	public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler) throws Exception {

		return this.handshakeHandler.doHandshake(request, response, webSocketHandler);
	}

}
