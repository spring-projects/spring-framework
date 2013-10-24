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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;

import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.endpoint.ServerEndpointRegistration;
import org.springframework.web.socket.server.endpoint.ServletServerContainerFactoryBean;

/**
 * Tomcat support for upgrading an {@link HttpServletRequest} during a WebSocket
 * handshake. To modify properties of the underlying
 * {@link javax.websocket.server.ServerContainer} you can use
 * {@link ServletServerContainerFactoryBean} in XML configuration or if using Java
 * configuration, access the container instance through the
 * "javax.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TomcatRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private List<WebSocketExtension> availableExtensions;

	@Override
	public String[] getSupportedVersions() {
		return new String[] { "13" };
	}

	@Override
	public List<WebSocketExtension> getAvailableExtensions(ServerHttpRequest request) {

		if(this.availableExtensions == null) {
			this.availableExtensions = new ArrayList<WebSocketExtension>();
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			for(Extension extension : getContainer(servletRequest).getInstalledExtensions()) {
				this.availableExtensions.add(parseStandardExtension(extension));
			}
		}
		return this.availableExtensions;
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String acceptedProtocol, Endpoint endpoint) throws HandshakeFailureException {

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isTrue(response instanceof ServletServerHttpResponse);
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

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

	public WsServerContainer getContainer(HttpServletRequest servletRequest) {
		String attribute = "javax.websocket.server.ServerContainer";
		ServletContext servletContext = servletRequest.getServletContext();
		return (WsServerContainer) servletContext.getAttribute(attribute);
	}

}