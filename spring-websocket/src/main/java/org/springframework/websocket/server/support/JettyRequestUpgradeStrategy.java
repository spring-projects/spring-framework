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

package org.springframework.websocket.server.support;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.ServletWebSocketRequest;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.adapter.JettyWebSocketListenerAdapter;
import org.springframework.websocket.server.RequestUpgradeStrategy;

/**
 * {@link RequestUpgradeStrategy} for use with Jetty. Based on Jetty's internal
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

	private static final String HANDLER_PROVIDER_ATTR_NAME = JettyRequestUpgradeStrategy.class.getName()
			+ ".HANDLER_PROVIDER";

	private WebSocketServerFactory factory;


	public JettyRequestUpgradeStrategy() {
		this.factory = new WebSocketServerFactory();
		this.factory.setCreator(new WebSocketCreator() {
			@Override
			public Object createWebSocket(UpgradeRequest request, UpgradeResponse response) {
				Assert.isInstanceOf(ServletWebSocketRequest.class, request);
				ServletWebSocketRequest servletRequest = (ServletWebSocketRequest) request;
				WebSocketHandler<?> webSocketHandler =
						(WebSocketHandler<?>) servletRequest.getServletAttributes().get(HANDLER_PROVIDER_ATTR_NAME);
				return new JettyWebSocketListenerAdapter(webSocketHandler);
			}
		});
		try {
			this.factory.init();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] { String.valueOf(HandshakeRFC6455.VERSION) };
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, WebSocketHandler<?> webSocketHandler) throws IOException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		upgrade(servletRequest, servletResponse, selectedProtocol, webSocketHandler);
	}

	private void upgrade(HttpServletRequest request, HttpServletResponse response,
			String selectedProtocol, final WebSocketHandler<?> webSocketHandler) throws IOException {

		Assert.state(this.factory.isUpgradeRequest(request, response), "Not a suitable WebSocket upgrade request");
		Assert.state(this.factory.acceptWebSocket(request, response), "Unable to accept WebSocket");

		request.setAttribute(HANDLER_PROVIDER_ATTR_NAME, webSocketHandler);
	}

}
