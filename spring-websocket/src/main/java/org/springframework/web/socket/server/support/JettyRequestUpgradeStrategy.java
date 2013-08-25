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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
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
import org.springframework.web.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.JettyWebSocketSession;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * {@link RequestUpgradeStrategy} for use with Jetty 9. Based on Jetty's internal
 * {@code org.eclipse.jetty.websocket.server.WebSocketHandler} class.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String WS_HANDLER_ATTR_NAME = JettyRequestUpgradeStrategy.class.getName() + ".WS_LISTENER";

	private WebSocketServerFactory factory;


	/**
	 * Default constructor that creates {@link WebSocketServerFactory} through its default
	 * constructor thus using a default {@link WebSocketPolicy}.
	 */
	public JettyRequestUpgradeStrategy() {
		this(new WebSocketServerFactory());
	}

	/**
	 * A constructor accepting a {@link WebSocketServerFactory}. This may be useful for
	 * modifying the factory's {@link WebSocketPolicy} via
	 * {@link WebSocketServerFactory#getPolicy()}.
	 */
	public JettyRequestUpgradeStrategy(WebSocketServerFactory factory) {
		this.factory.setCreator(new WebSocketCreator() {
			@Override
			public Object createWebSocket(UpgradeRequest request, UpgradeResponse response) {
				Assert.isInstanceOf(ServletUpgradeRequest.class, request);
				return ((ServletUpgradeRequest) request).getServletAttributes().get(WS_HANDLER_ATTR_NAME);
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
			String protocol, WebSocketHandler wsHandler, Map<String, Object> attrs) throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		Assert.isTrue(this.factory.isUpgradeRequest(servletRequest, servletResponse), "Not a WebSocket handshake");

		JettyWebSocketSession session = new JettyWebSocketSession(request.getPrincipal(), attrs);
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(wsHandler, session);

		try {
			servletRequest.setAttribute(WS_HANDLER_ATTR_NAME, handlerAdapter);
			this.factory.acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket, uri=" + request.getURI(), ex);
		}
		finally {
			servletRequest.removeAttribute(WS_HANDLER_ATTR_NAME);
		}
	}

}
