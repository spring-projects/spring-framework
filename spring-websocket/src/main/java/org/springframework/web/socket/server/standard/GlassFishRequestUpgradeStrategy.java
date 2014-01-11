/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.EndpointWrapper;
import org.glassfish.tyrus.core.ErrorCollector;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler;
import org.glassfish.tyrus.spi.SPIEndpoint;
import org.glassfish.tyrus.websockets.Connection;
import org.glassfish.tyrus.websockets.Version;
import org.glassfish.tyrus.websockets.WebSocketApplication;
import org.glassfish.tyrus.websockets.WebSocketEngine;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * {@code RequestUpgradeStrategy} that provides support for GlassFish 4 and beyond.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Michael Irwin
 * @since 4.0
 */
public class GlassFishRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private final static Random random = new Random();

	private static final Constructor<?> tyrusConnectionConstructor;

	private static final Constructor<?> tyrusEndpointConstructor;


	static {
		ClassLoader cl = GlassFishRequestUpgradeStrategy.class.getClassLoader();
		try {
			// Tyrus ConnectionImpl is package-visible only
			Class<?> tyrusConnectionClass = cl.loadClass("org.glassfish.tyrus.servlet.ConnectionImpl");
			tyrusConnectionConstructor = tyrusConnectionClass.getDeclaredConstructor(
					TyrusHttpUpgradeHandler.class, HttpServletResponse.class);
			ReflectionUtils.makeAccessible(tyrusConnectionConstructor);

			// TyrusEndpoint package location differs between GlassFish 4.0.0 and 4.0.1
			Class<?> tyrusEndpointClass;
			try {
				tyrusEndpointClass = cl.loadClass("org.glassfish.tyrus.core.TyrusEndpoint");
			}
			catch (ClassNotFoundException ex) {
				try {
					tyrusEndpointClass = cl.loadClass("org.glassfish.tyrus.server.TyrusEndpoint");
				}
				catch (ClassNotFoundException ex2) {
					// Propagate original exception for newer version of the class
					throw ex;
				}
			}
			tyrusEndpointConstructor = tyrusEndpointClass.getConstructor(SPIEndpoint.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible Tyrus version found", ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return StringUtils.commaDelimitedListToStringArray(Version.getSupportedWireProtocolVersions());
	}

	protected List<WebSocketExtension> getInstalledExtensions(WebSocketContainer container) {
		try {
			return super.getInstalledExtensions(container);
		}
		catch (UnsupportedOperationException e) {
			return new ArrayList<WebSocketExtension>();
		}
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> selectedExtensions,
			Endpoint endpoint) throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		WebSocketApplication webSocketApplication = createTyrusEndpoint(endpoint, selectedProtocol, selectedExtensions);

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
		catch (ServletException ex) {
			throw new HandshakeFailureException("Unable to create TyrusHttpUpgradeHandler", ex);
		}

		Connection connection = createConnection(upgradeHandler, response);

		RequestContext requestContext = RequestContext.Builder.create().
				requestURI(URI.create(wsApp.getPath())).requestPath(wsApp.getPath()).
				userPrincipal(request.getUserPrincipal()).
				connection(connection).secure(request.isSecure()).build();

		for (String header : headers.keySet()) {
			requestContext.getHeaders().put(header, headers.get(header));
		}

		boolean upgraded = WebSocketEngine.getEngine().upgrade(connection, requestContext,
				new WebSocketEngine.WebSocketHolderListener() {
					@Override
					public void onWebSocketHolder(WebSocketEngine.WebSocketHolder webSocketHolder) {
						upgradeHandler.setWebSocketHolder(webSocketHolder);
					}
				});

		// GlassFish bug ?? (see same line in TyrusServletFilter.doFilter)
		response.flushBuffer();

		return upgraded;
	}

	private WebSocketApplication createTyrusEndpoint(Endpoint endpoint, String selectedProtocol,
			List<Extension> selectedExtensions) {

		// Shouldn't matter for processing but must be unique
		String endpointPath = "/" + random.nextLong();
		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(endpointPath, endpoint);
		endpointConfig.setSubprotocols(Arrays.asList(selectedProtocol));
		endpointConfig.setExtensions(selectedExtensions);
		return createTyrusEndpoint(new EndpointWrapper(endpoint, endpointConfig,
				ComponentProviderService.create(), null, "/", new ErrorCollector(),
				endpointConfig.getConfigurator()));
	}

	private Connection createConnection(TyrusHttpUpgradeHandler handler, HttpServletResponse response) {
		try {
			return (Connection) tyrusConnectionConstructor.newInstance(handler, response);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create GlassFish connection", ex);
		}
	}

	protected WebSocketApplication createTyrusEndpoint(EndpointWrapper endpoint) {
		try {
			return (WebSocketApplication) tyrusEndpointConstructor.newInstance(endpoint);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create GlassFish endpoint", ex);
		}
	}

}
