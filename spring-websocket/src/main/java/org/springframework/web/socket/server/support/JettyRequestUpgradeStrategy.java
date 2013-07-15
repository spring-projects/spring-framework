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

package org.springframework.web.socket.server.support;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.JettyWebSocketListenerAdapter;
import org.springframework.web.socket.adapter.JettyWebSocketSessionAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * {@link RequestUpgradeStrategy} for use with Jetty 9. Based on Jetty's internal
 * {@code org.eclipse.jetty.websocket.server.WebSocketHandler} class.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	// FIXME jetty has options, timeouts etc. Do we need a common abstraction

	// FIXME need a way for someone to plug their own RequestUpgradeStrategy or override
	// Jetty settings

	// FIXME when to call factory.cleanup();

	private static final String WEBSOCKET_LISTENER_ATTR_NAME = JettyRequestUpgradeStrategy.class.getName()
			+ ".HANDLER_PROVIDER";

	private WebSocketServerFactory factory;

	private final ServerWebSocketSessionInitializer wsSessionInitializer = new ServerWebSocketSessionInitializer();


	public JettyRequestUpgradeStrategy() {
		this.factory = new WebSocketServerFactory();
		this.factory.setCreator(new WebSocketCreator() {
			@Override
			public Object createWebSocket(UpgradeRequest request, UpgradeResponse response) {
				Assert.isInstanceOf(ServletUpgradeRequest.class, request);
				return ((ServletUpgradeRequest) request).getServletAttributes().get(WEBSOCKET_LISTENER_ATTR_NAME);
			}
		});
		try {
			this.factory.init();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Jetty WebSocketServerFactory", ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] { String.valueOf(HandshakeRFC6455.VERSION) };
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, WebSocketHandler webSocketHandler) throws IOException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		if (!this.factory.isUpgradeRequest(servletRequest, servletResponse)) {
			// should never happen
			throw new HandshakeFailureException("Not a WebSocket request");
		}

		JettyWebSocketSessionAdapter session = new JettyWebSocketSessionAdapter();
		this.wsSessionInitializer.initialize(request, response, session);
		JettyWebSocketListenerAdapter listener = new JettyWebSocketListenerAdapter(webSocketHandler, session);

		servletRequest.setAttribute(WEBSOCKET_LISTENER_ATTR_NAME, listener);

		if (!this.factory.acceptWebSocket(servletRequest, servletResponse)) {
			// should never happen
			throw new HandshakeFailureException("WebSocket request not accepted by Jetty");
		}
	}

}
