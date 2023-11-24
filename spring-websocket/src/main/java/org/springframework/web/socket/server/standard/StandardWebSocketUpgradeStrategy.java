/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server.standard;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for the Jakarta WebSocket API 2.1+.
 *
 * <p>This strategy serves as a fallback if no specific server has been detected.
 * It can also be used with Jakarta EE 10 level servers such as Tomcat 10.1 and
 * Undertow 2.3 directly, relying on their built-in Jakarta WebSocket 2.1 support.
 *
 * <p>To modify properties of the underlying {@link jakarta.websocket.server.ServerContainer}
 * you can use {@link ServletServerContainerFactoryBean} in XML configuration or,
 * when using Java configuration, access the container instance through the
 * "jakarta.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see jakarta.websocket.server.ServerContainer#upgradeHttpToWebSocket
 */
public class StandardWebSocketUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private static final String[] SUPPORTED_VERSIONS = new String[] {"13"};


	@Override
	public String[] getSupportedVersions() {
		return SUPPORTED_VERSIONS;
	}

	@Override
	protected void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
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
			upgradeHttpToWebSocket(servletRequest, servletResponse, endpointConfig, pathParams);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket: " + requestUrl, ex);
		}
	}

	protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
			ServerEndpointConfig endpointConfig, Map<String,String> pathParams) throws Exception {

		getContainer(request).upgradeHttpToWebSocket(request, response, endpointConfig, pathParams);
	}

}
