/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * WebSphere support for upgrading an {@link HttpServletRequest} during a
 * WebSocket handshake. To modify properties of the underlying
 * {@link javax.websocket.server.ServerContainer} you can use
 * {@link ServletServerContainerFactoryBean} in XML configuration or, when using
 * Java configuration, access the container instance through the
 * "javax.websocket.server.ServerContainer" ServletContext attribute.
 *
 * <p>Tested with WAS Liberty beta (August 2015) for the upcoming 8.5.5.7 release.
 *
 * @author Rossen Stoyanchev
 * @since 4.2.1
 */
public class WebSphereRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private final static Method upgradeMethod;

	static {
		ClassLoader loader = WebSphereRequestUpgradeStrategy.class.getClassLoader();
		try {
			Class<?> type = loader.loadClass("com.ibm.websphere.wsoc.WsWsocServerContainer");
			upgradeMethod = type.getMethod("doUpgrade", HttpServletRequest.class,
					HttpServletResponse.class, ServerEndpointConfig.class, Map.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible WebSphere version found", ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] {"13"};
	}

	@Override
	public void upgradeInternal(ServerHttpRequest httpRequest, ServerHttpResponse httpResponse,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException {

		HttpServletRequest request = getHttpServletRequest(httpRequest);
		HttpServletResponse response = getHttpServletResponse(httpResponse);

		StringBuffer requestUrl = request.getRequestURL();
		String path = request.getRequestURI();  // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
		endpointConfig.setExtensions(selectedExtensions);

		try {
			ServerContainer container = getContainer(request);
			upgradeMethod.invoke(container, request, response, endpointConfig, pathParams);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket for " + requestUrl, ex);
		}
	}

}
