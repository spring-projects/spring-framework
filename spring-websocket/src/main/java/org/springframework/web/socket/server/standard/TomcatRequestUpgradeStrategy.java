/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;

import org.apache.tomcat.websocket.server.WsServerContainer;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Apache Tomcat. Compatible with
 * all versions of Tomcat that support JSR-356, i.e. Tomcat 7.0.47+ and higher.
 *
 * <p>To modify properties of the underlying {@link javax.websocket.server.ServerContainer}
 * you can use {@link ServletServerContainerFactoryBean} in XML configuration or,
 * when using Java configuration, access the container instance through the
 * "javax.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TomcatRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	@Override
	public String[] getSupportedVersions() {
		return new String[] {"13"};
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		StringBuffer requestUrl = servletRequest.getRequestURL();
		String path = servletRequest.getRequestURI();  // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
		endpointConfig.setExtensions(selectedExtensions);

		try {
			getContainer(servletRequest).doUpgrade(servletRequest, servletResponse, endpointConfig, pathParams);
		}
		catch (ServletException ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket: " + requestUrl, ex);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + requestUrl, ex);
		}
	}

	public WsServerContainer getContainer(HttpServletRequest request) {
		return (WsServerContainer) super.getContainer(request);
	}

}
