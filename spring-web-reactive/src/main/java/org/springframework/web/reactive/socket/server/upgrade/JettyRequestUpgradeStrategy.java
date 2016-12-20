/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.socket.server.upgrade;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServletServerHttpRequest;
import org.springframework.http.server.reactive.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with Jetty.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, Lifecycle {

	private static final ThreadLocal<WebSocketHandlerContainer> adapterHolder =
			new NamedThreadLocal<>("JettyWebSocketHandlerAdapter");


	private WebSocketServerFactory factory;

	private ServletContext servletContext;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning() && this.servletContext != null) {
				this.running = true;
				try {
					this.factory = new WebSocketServerFactory(this.servletContext);
					this.factory.setCreator((request, response) -> {
						WebSocketHandlerContainer container = adapterHolder.get();
						String protocol = container.getProtocol().orElse(null);
						if (protocol != null) {
							response.setAcceptedSubProtocol(protocol);
						}
						return container.getAdapter();
					});
					this.factory.start();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to start WebSocketServerFactory", ex);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				this.running = false;
				try {
					this.factory.stop();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to stop WebSocketServerFactory", ex);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			Optional<String> subProtocol) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		HandshakeInfo info = getHandshakeInfo(exchange, subProtocol);
		DataBufferFactory factory = response.bufferFactory();
		JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(handler, info, factory);

		startLazily(servletRequest);

		boolean isUpgrade = this.factory.isUpgradeRequest(servletRequest, servletResponse);
		Assert.isTrue(isUpgrade, "Not a WebSocket handshake");

		try {
			adapterHolder.set(new WebSocketHandlerContainer(adapter, subProtocol));
			this.factory.acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
		finally {
			adapterHolder.remove();
		}

		return Mono.empty();
	}

	private HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
		Assert.isTrue(request instanceof ServletServerHttpRequest);
		return ((ServletServerHttpRequest) request).getServletRequest();
	}

	private HttpServletResponse getHttpServletResponse(ServerHttpResponse response) {
		Assert.isTrue(response instanceof ServletServerHttpResponse);
		return ((ServletServerHttpResponse) response).getServletResponse();
	}

	private HandshakeInfo getHandshakeInfo(ServerWebExchange exchange, Optional<String> protocol) {
		ServerHttpRequest request = exchange.getRequest();
		Mono<Principal> principal = exchange.getPrincipal();
		return new HandshakeInfo(request.getURI(), request.getHeaders(), principal, protocol);
	}

	private void startLazily(HttpServletRequest request) {
		if (this.servletContext != null) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			if (this.servletContext == null) {
				this.servletContext = request.getServletContext();
				this.servletContext.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
				start();
			}
		}
	}


	private static class WebSocketHandlerContainer {

		private final JettyWebSocketHandlerAdapter adapter;

		private final Optional<String> protocol;


		public WebSocketHandlerContainer(JettyWebSocketHandlerAdapter adapter, Optional<String> protocol) {
			this.adapter = adapter;
			this.protocol = protocol;
		}

		public JettyWebSocketHandlerAdapter getAdapter() {
			return this.adapter;
		}

		public Optional<String> getProtocol() {
			return this.protocol;
		}
	}
}
