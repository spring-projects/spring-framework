/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.server.jetty;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.adapter.jetty.WebSocketToJettyExtensionConfigAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A {@link RequestUpgradeStrategy} for use with Jetty 9.1-9.3. Based on Jetty's
 * internal {@code org.eclipse.jetty.websocket.server.WebSocketHandler} class.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, Lifecycle, ServletContextAware {

	private static final ThreadLocal<WebSocketHandlerContainer> wsContainerHolder =
			new NamedThreadLocal<WebSocketHandlerContainer>("WebSocket Handler Container");

	// Actually 9.3.15+
	private static boolean isJetty94 = ClassUtils.hasConstructor(WebSocketServerFactory.class, ServletContext.class);

	private static boolean isJetty91 = ClassUtils.hasMethod(WebSocketServerFactory.class, "init");

	private WebSocketServerFactoryAdapter factoryAdapter;

	private volatile List<WebSocketExtension> supportedExtensions;

	protected ServletContext servletContext;

	private volatile boolean running = false;

	/**
	 * Default constructor that creates {@link WebSocketServerFactory} through
	 * its default constructor thus using a default {@link WebSocketPolicy}.
	 */
	public JettyRequestUpgradeStrategy() {
		this(WebSocketPolicy.newServerPolicy());
	}

	/**
	 * A constructor accepting a {@link WebSocketPolicy}
	 * to be used when creating the {@link WebSocketServerFactory} instance.
	 * @since 4.3
	 */
	public JettyRequestUpgradeStrategy(WebSocketPolicy webSocketPolicy) {
		this.factoryAdapter = isJetty94 ? new Jetty94WebSocketServerFactoryAdapter()
				: new JettyWebSocketServerFactoryAdapter();
		this.factoryAdapter.setWebSocketPolicy(webSocketPolicy);
	}

	@Override
	public String[] getSupportedVersions() {
		return new String[] {String.valueOf(HandshakeRFC6455.VERSION)};
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
		for (String name : this.factoryAdapter.getFactory().getExtensionFactory().getExtensionNames()) {
			result.add(new WebSocketExtension(name));
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			try {
				this.factoryAdapter.start();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to start Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			try {
				this.running = false;
				factoryAdapter.stop();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to stop Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<WebSocketExtension> selectedExtensions, Principal user,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		Assert.isTrue(this.factoryAdapter.getFactory()
				.isUpgradeRequest(servletRequest, servletResponse), "Not a WebSocket handshake");

		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(wsHandler, session);

		WebSocketHandlerContainer container =
				new WebSocketHandlerContainer(handlerAdapter, selectedProtocol, selectedExtensions);

		try {
			wsContainerHolder.set(container);
			this.factoryAdapter.getFactory().acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		}
		finally {
			wsContainerHolder.remove();
		}
	}


	private static class WebSocketHandlerContainer {

		private final JettyWebSocketHandlerAdapter handler;

		private final String selectedProtocol;

		private final List<ExtensionConfig> extensionConfigs;

		public WebSocketHandlerContainer(JettyWebSocketHandlerAdapter handler, String protocol, List<WebSocketExtension> extensions) {
			this.handler = handler;
			this.selectedProtocol = protocol;
			if (CollectionUtils.isEmpty(extensions)) {
				this.extensionConfigs = new ArrayList<ExtensionConfig>();
			}
			else {
				this.extensionConfigs = new ArrayList<ExtensionConfig>();
				for (WebSocketExtension e : extensions) {
					this.extensionConfigs.add(new WebSocketToJettyExtensionConfigAdapter(e));
				}
			}
		}

		public JettyWebSocketHandlerAdapter getHandler() {
			return this.handler;
		}

		public String getSelectedProtocol() {
			return this.selectedProtocol;
		}

		public List<ExtensionConfig> getExtensionConfigs() {
			return this.extensionConfigs;
		}
	}

	private static abstract class WebSocketServerFactoryAdapter {

		protected WebSocketServerFactory factory;

		protected WebSocketPolicy webSocketPolicy;

		public WebSocketServerFactory getFactory() {
			return factory;
		}

		public void setWebSocketPolicy(WebSocketPolicy webSocketPolicy) {
			this.webSocketPolicy = webSocketPolicy;
		}

		protected void configureFactory() {
			this.factory.setCreator(new WebSocketCreator() {
				@Override
				public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
					WebSocketHandlerContainer container = wsContainerHolder.get();
					Assert.state(container != null, "Expected WebSocketHandlerContainer");
					response.setAcceptedSubProtocol(container.getSelectedProtocol());
					response.setExtensions(container.getExtensionConfigs());
					return container.getHandler();
				}
			});
		}

		abstract void start() throws Exception;

		abstract void stop() throws Exception;
	}

	private class JettyWebSocketServerFactoryAdapter extends WebSocketServerFactoryAdapter {

		@Override
		void start() throws Exception {
			this.factory = WebSocketServerFactory.class.getConstructor(WebSocketPolicy.class)
					.newInstance(this.webSocketPolicy);
			configureFactory();
			if(isJetty91) {
				WebSocketServerFactory.class.getMethod("init").invoke(this.factory);
			}
			else {
				WebSocketServerFactory.class.getMethod("init", ServletContext.class)
						.invoke(this.factory, servletContext);
			}
		}

		@Override
		void stop() throws Exception {
			WebSocketServerFactory.class.getMethod("cleanup").invoke(this.factory);
		}
	}

	private class Jetty94WebSocketServerFactoryAdapter extends WebSocketServerFactoryAdapter {

		@Override
		void start() throws Exception {
			servletContext.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
			this.factory = new WebSocketServerFactory(servletContext, this.webSocketPolicy);
			configureFactory();
			this.factory.start();
		}

		@Override
		void stop() throws Exception {
			this.factory.stop();
		}
	}

}
