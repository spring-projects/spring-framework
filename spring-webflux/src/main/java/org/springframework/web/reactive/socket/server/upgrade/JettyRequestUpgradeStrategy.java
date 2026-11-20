/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.websocket.api.Configurable;
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
 * A WebSocket {@code RequestUpgradeStrategy} for Jetty 12 EE11.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private @Nullable Consumer<Configurable> webSocketConfigurer;

	private @Nullable JettyWebSocketServerContainer serverContainer;


	/**
	 * Add a callback to configure WebSocket server parameters on
	 * {@link JettyWebSocketServerContainer}.
	 * @since 6.1
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

					JettyWebSocketServerContainer container = getWebSocketServerContainer(servletContext);
					try {
						container.upgrade(webSocketCreator, servletRequest, servletResponse);
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}

					return Mono.empty();
				}));
	}

	private JettyWebSocketServerContainer getWebSocketServerContainer(ServletContext servletContext) {
		if (this.serverContainer == null) {
			JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);
			if (this.webSocketConfigurer != null) {
				this.webSocketConfigurer.accept(container);
			}
			this.serverContainer = container;
		}
		return this.serverContainer;
	}

}
