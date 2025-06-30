/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.api.Configurable;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Jetty 12 Core.
 *
 * @author Rossen Stoyanchev
 * @author Greg Wilkins
 * @since 6.2
 */
public class JettyCoreRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private @Nullable Consumer<Configurable> webSocketConfigurer;

	private @Nullable ServerWebSocketContainer serverContainer;

	/**
	 * Add a callback to configure WebSocket server parameters on
	 * {@link JettyWebSocketServerContainer}.
	 */
	public void addWebSocketConfigurer(Consumer<Configurable> webSocketConfigurer) {
		this.webSocketConfigurer = (this.webSocketConfigurer != null ?
				this.webSocketConfigurer.andThen(webSocketConfigurer) : webSocketConfigurer);
	}

	@Override
	public Mono<Void> upgrade(
			ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		Request jettyRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		Response jettyResponse = ServerHttpResponseDecorator.getNativeResponse(response);

		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory factory = response.bufferFactory();

		// Trigger WebFlux preCommit actions before upgrade
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new JettyWebSocketSession(session, handshakeInfo, factory));

					WebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse, callback) -> {
						if (subProtocol != null) {
							upgradeResponse.setAcceptedSubProtocol(subProtocol);
						}
						return adapter;
					};

					Callback.Completable callback = new Callback.Completable();
					Mono<Void> mono = Mono.fromFuture(callback);
					ServerWebSocketContainer container = getWebSocketServerContainer(jettyRequest);
					try {
						if (!container.upgrade(webSocketCreator, jettyRequest, jettyResponse, callback)) {
							throw new WebSocketException("request could not be upgraded to websocket");
						}
					}
					catch (WebSocketException ex) {
						callback.failed(ex);
					}

					return mono;
				}));
	}

	private ServerWebSocketContainer getWebSocketServerContainer(Request jettyRequest) {
		if (this.serverContainer == null) {
			Server server = jettyRequest.getConnectionMetaData().getConnector().getServer();
			ServerWebSocketContainer container = ServerWebSocketContainer.get(server.getContext());
			if (this.webSocketConfigurer != null) {
				this.webSocketConfigurer.accept(container);
			}
			this.serverContainer = container;
		}
		return this.serverContainer;
	}

}
