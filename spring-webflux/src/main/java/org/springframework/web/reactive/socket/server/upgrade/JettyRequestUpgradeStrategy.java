/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.function.Supplier;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for Jetty 11.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	@Override
	public Mono<Void> upgrade(
			ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		ServletContext servletContext = servletRequest.getServletContext();

		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory factory = response.bufferFactory();

		// Trigger WebFlux preCommit actions before upgrade
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new JettyWebSocketSession(session, handshakeInfo, factory));

					JettyWebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse) -> {
						if (subProtocol != null) {
							upgradeResponse.setAcceptedSubProtocol(subProtocol);
						}
						return adapter;
					};

					JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);

					try {
						container.upgrade(webSocketCreator, servletRequest, servletResponse);
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}

					return Mono.empty();
				}));
	}

}
