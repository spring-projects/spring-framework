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
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Arrays;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.EndpointWrapper;
import org.glassfish.tyrus.core.ErrorCollector;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler;
import org.glassfish.tyrus.websockets.Connection;
import org.glassfish.tyrus.websockets.Version;
import org.glassfish.tyrus.websockets.WebSocketApplication;
import org.glassfish.tyrus.websockets.WebSocketEngine;
import org.glassfish.tyrus.websockets.WebSocketEngine.WebSocketHolderListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.endpoint.ServerEndpointRegistration;
import org.springframework.web.socket.server.endpoint.ServletServerContainerFactoryBean;


/**
 * GlassFish support for upgrading a request during a WebSocket handshake. To modify
 * properties of the underlying {@link javax.websocket.server.ServerContainer} you can use
 * {@link ServletServerContainerFactoryBean} in XML configuration or if using Java
 * configuration, access the container instance through the
 * "javax.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractGlassFishRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private final static Random random = new Random();

	@Override
	public String[] getSupportedVersions() {
		return StringUtils.commaDelimitedListToStringArray(Version.getSupportedWireProtocolVersions());
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, Endpoint endpoint) throws HandshakeFailureException {

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isTrue(response instanceof ServletServerHttpResponse);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		WebSocketApplication webSocketApplication = createTyrusEndpoint(servletRequest, endpoint, selectedProtocol);
		WebSocketEngine webSocketEngine = WebSocketEngine.getEngine();

		try {
			webSocketEngine.register(webSocketApplication);
		}
		catch (DeploymentException ex) {
			throw new HandshakeFailureException("Failed to configure endpoint in GlassFish", ex);
		}

		try {
			performUpgrade(servletRequest, servletResponse, request.getHeaders(), webSocketApplication);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket, uri=" + request.getURI(), ex);
		}
		finally {
			webSocketEngine.unregister(webSocketApplication);
		}
	}

	private boolean performUpgrade(HttpServletRequest request, HttpServletResponse response,
			HttpHeaders headers, WebSocketApplication wsApp) throws IOException {

		final TyrusHttpUpgradeHandler upgradeHandler;
		try {
			upgradeHandler = request.upgrade(TyrusHttpUpgradeHandler.class);
		}
		catch (ServletException e) {
			throw new HandshakeFailureException("Unable to create UpgardeHandler", e);
		}

		Connection connection = createConnection(upgradeHandler, response);

		RequestContext wsRequest = RequestContext.Builder.create()
				.requestURI(URI.create(wsApp.getPath())).requestPath(wsApp.getPath())
				.connection(connection).secure(request.isSecure()).build();

		for (String header : headers.keySet()) {
			wsRequest.getHeaders().put(header, headers.get(header));
		}

		return WebSocketEngine.getEngine().upgrade(connection, wsRequest, new WebSocketHolderListener() {
			@Override
			public void onWebSocketHolder(WebSocketEngine.WebSocketHolder webSocketHolder) {
				upgradeHandler.setWebSocketHolder(webSocketHolder);
			}
		});
	}

	private WebSocketApplication createTyrusEndpoint(HttpServletRequest request,
			Endpoint endpoint, String selectedProtocol) {

		// shouldn't matter for processing but must be unique
		String endpointPath = "/" + random.nextLong();
		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(endpointPath, endpoint);
		endpointConfig.setSubprotocols(Arrays.asList(selectedProtocol));
		return createTyrusEndpoint(new EndpointWrapper(endpoint, endpointConfig,
				ComponentProviderService.create(), null, "/", new ErrorCollector(),
				endpointConfig.getConfigurator()));
	}

	/**
	 * Create the actual TyrusEndpoint
	 * @param endpoint The WebSocket endpoint
	 * @return The configured WebSocketApplication, most likely {@code TyrusEndpoint}
	 */
	abstract protected WebSocketApplication createTyrusEndpoint(EndpointWrapper endpoint);

	private Connection createConnection(TyrusHttpUpgradeHandler handler, HttpServletResponse response) {
		try {
			String name = "org.glassfish.tyrus.servlet.ConnectionImpl";
			Class<?> clazz = ClassUtils.forName(name, GlassFishRequestUpgradeStrategy.class.getClassLoader());
			Constructor<?> constructor = clazz.getDeclaredConstructor(TyrusHttpUpgradeHandler.class, HttpServletResponse.class);
			ReflectionUtils.makeAccessible(constructor);
			return (Connection) constructor.newInstance(handler, response);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to instantiate GlassFish connection", ex);
		}
	}

}
