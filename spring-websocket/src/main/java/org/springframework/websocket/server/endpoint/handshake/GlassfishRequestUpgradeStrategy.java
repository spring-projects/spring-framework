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

package org.springframework.websocket.server.endpoint.handshake;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.websocket.Endpoint;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.EndpointWrapper;
import org.glassfish.tyrus.core.ErrorCollector;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.server.TyrusEndpoint;
import org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler;
import org.glassfish.tyrus.websockets.Connection;
import org.glassfish.tyrus.websockets.Version;
import org.glassfish.tyrus.websockets.WebSocketEngine;
import org.glassfish.tyrus.websockets.WebSocketEngine.WebSocketHolderListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.websocket.server.endpoint.EndpointRegistration;
import org.springframework.websocket.server.endpoint.EndpointRequestUpgradeStrategy;

/**
 * Glassfish support for upgrading an {@link HttpServletRequest} during a WebSocket
 * handshake.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class GlassfishRequestUpgradeStrategy implements EndpointRequestUpgradeStrategy {

	private final static Random random = new Random();


	@Override
	public String[] getSupportedVersions() {
		return StringUtils.commaDelimitedListToStringArray(Version.getSupportedWireProtocolVersions());
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response, String protocol,
			Endpoint endpoint) throws Exception {

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isTrue(response instanceof ServletServerHttpResponse);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
		servletResponse = new AlreadyUpgradedResponseWrapper(servletResponse);

		TyrusEndpoint tyrusEndpoint = createTyrusEndpoint(servletRequest, endpoint);
		WebSocketEngine.getEngine().register(tyrusEndpoint);

		try {
			if (!performUpgrade(servletRequest, servletResponse, request.getHeaders(), tyrusEndpoint)) {
				throw new IllegalStateException("Failed to upgrade HttpServletRequest");
			}
		}
		finally {
			WebSocketEngine.getEngine().unregister(tyrusEndpoint);
		}
	}

	private boolean performUpgrade(HttpServletRequest request, HttpServletResponse response,
			HttpHeaders headers, TyrusEndpoint tyrusEndpoint) throws Exception {

		final TyrusHttpUpgradeHandler upgradeHandler = request.upgrade(TyrusHttpUpgradeHandler.class);

		Connection connection = createConnection(upgradeHandler, response);

		RequestContext wsRequest = RequestContext.Builder.create()
				.requestURI(URI.create(tyrusEndpoint.getPath())).requestPath(tyrusEndpoint.getPath())
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

	private TyrusEndpoint createTyrusEndpoint(HttpServletRequest request, Endpoint endpoint) {

		// Use randomized path
		String requestUri = request.getRequestURI();
		String randomValue = String.valueOf(random.nextLong());
		String endpointPath = requestUri.endsWith("/") ? requestUri + randomValue : requestUri + "/" + randomValue;

		EndpointRegistration endpointConfig = new EndpointRegistration(endpointPath, endpoint);

		return new TyrusEndpoint(new EndpointWrapper(endpoint, endpointConfig,
				ComponentProviderService.create(), null, "/", new ErrorCollector(),
				endpointConfig.getConfigurator()));
	}

	private Connection createConnection(TyrusHttpUpgradeHandler handler, HttpServletResponse response) throws Exception {
		String name = "org.glassfish.tyrus.servlet.ConnectionImpl";
		Class<?> clazz = ClassUtils.forName(name, GlassfishRequestUpgradeStrategy.class.getClassLoader());
		Constructor<?> constructor = clazz.getDeclaredConstructor(TyrusHttpUpgradeHandler.class, HttpServletResponse.class);
		ReflectionUtils.makeAccessible(constructor);
		return (Connection) constructor.newInstance(handler, response);
	}


	private static class AlreadyUpgradedResponseWrapper extends HttpServletResponseWrapper {

		public AlreadyUpgradedResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setStatus(int sc) {
			Assert.isTrue(sc == HttpStatus.SWITCHING_PROTOCOLS.value(), "Unexpected status code " + sc);
		}
		@Override
		public void addHeader(String name, String value) {
			// ignore
		}
	}

}
