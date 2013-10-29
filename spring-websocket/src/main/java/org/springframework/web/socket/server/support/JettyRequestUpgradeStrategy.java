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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.support.WebSocketExtension;
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

	private static final ThreadLocal<WebSocketHandlerContainer> wsContainerHolder =
			new NamedThreadLocal<WebSocketHandlerContainer>("WebSocket Handler Container");


	private WebSocketServerFactory factory;

	private volatile List<WebSocketExtension> supportedExtensions;


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
		Assert.notNull(factory, "WebSocketServerFactory is required");
		this.factory = factory;
		this.factory.setCreator(new WebSocketCreator() {

			@Override
			public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
				// Cast to avoid infinite recursion
				return createWebSocket((UpgradeRequest) request, (UpgradeResponse) response);
			}

			// For Jetty 9.0.x
			public Object createWebSocket(UpgradeRequest request, UpgradeResponse response) {

				WebSocketHandlerContainer container = wsContainerHolder.get();
				Assert.state(container != null, "Expected WebSocketHandlerContainer");

				response.setAcceptedSubProtocol(container.getSelectedProtocol());
				response.setExtensions(container.getExtensionConfigs());

				return container.getHandler();
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
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		if (this.supportedExtensions == null) {
			this.supportedExtensions = getWebSocketExtensions();
		}
		return this.supportedExtensions;
	}

	private List<WebSocketExtension> getWebSocketExtensions() {
		List<WebSocketExtension> result = new ArrayList<WebSocketExtension>();
		for(String name : this.factory.getExtensionFactory().getExtensionNames()) {
			result.add(new WebSocketExtension(name));
		}
		return result;
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		Assert.isTrue(this.factory.isUpgradeRequest(servletRequest, servletResponse), "Not a WebSocket handshake");

		JettyWebSocketSession session = new JettyWebSocketSession(request.getPrincipal(), attributes);
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(wsHandler, session);

		WebSocketHandlerContainer container =
				new WebSocketHandlerContainer(handlerAdapter, selectedProtocol, selectedExtensions);

		try {
			wsContainerHolder.set(container);
			this.factory.acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket, uri=" + request.getURI(), ex);
		}
		finally {
			wsContainerHolder.remove();
		}
	}


	private static class WebSocketHandlerContainer {

		private final JettyWebSocketHandlerAdapter handler;

		private final String selectedProtocol;

		private final List<ExtensionConfig> extensionConfigs;

		private WebSocketHandlerContainer(JettyWebSocketHandlerAdapter handler, String protocol,
				List<WebSocketExtension> extensions) {

			this.handler = handler;
			this.selectedProtocol = protocol;

			if (CollectionUtils.isEmpty(extensions)) {
				this.extensionConfigs = null;
			}
			else {
				this.extensionConfigs = new ArrayList<ExtensionConfig>();
				for (WebSocketExtension e : extensions) {
					this.extensionConfigs.add(new WebSocketExtension.WebSocketToJettyExtensionConfigAdapter(e));
				}
			}
		}

		private JettyWebSocketHandlerAdapter getHandler() {
			return this.handler;
		}

		private String getSelectedProtocol() {
			return this.selectedProtocol;
		}

		private List<ExtensionConfig> getExtensionConfigs() {
			return this.extensionConfigs;
		}
	}
}
