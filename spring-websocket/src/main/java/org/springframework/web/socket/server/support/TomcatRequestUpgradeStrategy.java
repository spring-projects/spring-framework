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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.apache.tomcat.websocket.server.WsHttpUpgradeHandler;
import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.endpoint.ServerEndpointRegistration;

/**
 * Tomcat support for upgrading an {@link HttpServletRequest} during a WebSocket handshake.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TomcatRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {


	@Override
	public String[] getSupportedVersions() {
		return new String[] { "13" };
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String acceptedProtocol, Endpoint endpoint) throws HandshakeFailureException {

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isTrue(response instanceof ServletServerHttpResponse);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		if (hasDoUpgrade) {
			doUpgrade(servletRequest, servletResponse, acceptedProtocol, endpoint);
		}
		else {
			upgradeTomcat80RC1(servletRequest, acceptedProtocol, endpoint);
		}
	}

	private void doUpgrade(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			String acceptedProtocol, Endpoint endpoint) {

		StringBuffer requestUrl = servletRequest.getRequestURL();
		String path = servletRequest.getRequestURI(); // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Arrays.asList(acceptedProtocol));

		try {
			getContainer(servletRequest).doUpgrade(servletRequest, servletResponse, endpointConfig, pathParams);
		}
		catch (ServletException ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket, uri=" + requestUrl, ex);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket, uri=" + requestUrl, ex);
		}
	}

	private WsServerContainer getContainer(HttpServletRequest servletRequest) {
		String attribute = "javax.websocket.server.ServerContainer";
		ServletContext servletContext = servletRequest.getServletContext();
		return (WsServerContainer) servletContext.getAttribute(attribute);
	}

	// FIXME: Remove this after RC2 is out

	private void upgradeTomcat80RC1(HttpServletRequest request, String protocol, Endpoint endpoint) {

		WsHttpUpgradeHandler upgradeHandler;
		try {
			upgradeHandler = request.upgrade(WsHttpUpgradeHandler.class);
		}
		catch (Exception e) {
			throw new HandshakeFailureException("Unable to create UpgardeHandler", e);
		}

		WsHandshakeRequest webSocketRequest = new WsHandshakeRequest(request);
		try {
			Method method = ReflectionUtils.findMethod(WsHandshakeRequest.class, "finished");
			ReflectionUtils.makeAccessible(method);
			method.invoke(webSocketRequest);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Failed to upgrade HttpServletRequest", ex);
		}

		ServerEndpointConfig endpointConfig = new ServerEndpointRegistration("/shouldntmatter", endpoint);

		upgradeHandler.preInit(endpoint, endpointConfig, getContainer(request), webSocketRequest,
				protocol, Collections.<String, String> emptyMap(), request.isSecure());
	}

	private static boolean hasDoUpgrade = (ReflectionUtils.findMethod(WsServerContainer.class,
			"doUpgrade", HttpServletRequest.class, HttpServletResponse.class,
			ServerEndpointConfig.class, Map.class) != null);

}