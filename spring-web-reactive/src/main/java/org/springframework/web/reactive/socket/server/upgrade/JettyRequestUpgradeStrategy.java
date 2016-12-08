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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServletServerHttpRequest;
import org.springframework.http.server.reactive.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * A {@link RequestUpgradeStrategy} for use with Jetty.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, Lifecycle {

	private static final ThreadLocal<JettyWebSocketHandlerAdapter> wsContainerHolder =
			new NamedThreadLocal<>("Jetty WebSocketHandler Adapter");

	private WebSocketServerFactory factory;

	private ServletContext servletContext;

	private volatile boolean running = false;

	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {

		JettyWebSocketHandlerAdapter adapter =
				new JettyWebSocketHandlerAdapter(webSocketHandler);

		HttpServletRequest servletRequest = getHttpServletRequest(exchange.getRequest());
		HttpServletResponse servletResponse = getHttpServletResponse(exchange.getResponse());

		if (this.servletContext == null) {
			this.servletContext = servletRequest.getServletContext();
			servletContext.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
		}

		try {
			start();

			Assert.isTrue(this.factory.isUpgradeRequest(servletRequest, servletResponse), "Not a WebSocket handshake");

			wsContainerHolder.set(adapter);
			this.factory.acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
		finally {
			wsContainerHolder.remove();
		}

		return Mono.empty();
	}

	@Override
	public void start() {
		if (!isRunning() && this.servletContext != null) {
			this.running = true;
			try {
				this.factory = new WebSocketServerFactory(this.servletContext);
				this.factory.setCreator(new WebSocketCreator() {

					@Override
					public Object createWebSocket(ServletUpgradeRequest req,
							ServletUpgradeResponse resp) {
						JettyWebSocketHandlerAdapter adapter = wsContainerHolder.get();
						Assert.state(adapter != null, "Expected JettyWebSocketHandlerAdapter");
						return adapter;
					}

				});
				this.factory.start();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to start Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			try {
				this.factory.stop();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to stop Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	private final HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
		Assert.isTrue(request instanceof ServletServerHttpRequest);
		return ((ServletServerHttpRequest) request).getServletRequest();
	}

	private final HttpServletResponse getHttpServletResponse(ServerHttpResponse response) {
		Assert.isTrue(response instanceof ServletServerHttpResponse);
		return ((ServletServerHttpResponse) response).getServletResponse();
	}

}
