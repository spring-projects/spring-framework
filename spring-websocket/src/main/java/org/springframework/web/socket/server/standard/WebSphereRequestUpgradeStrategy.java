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

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSphere support for upgrading an {@link HttpServletRequest} during a
 * WebSocket handshake. To modify properties of the underlying
 * {@link jakarta.websocket.server.ServerContainer} you can use
 * {@link ServletServerContainerFactoryBean} in XML configuration or, when using
 * Java configuration, access the container instance through the
 * "javax.websocket.server.ServerContainer" ServletContext attribute.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2.1
 */
public class WebSphereRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

	private static final Method upgradeMethod;

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
	protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
			ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

		ServerContainer container = getContainer(request);
		upgradeMethod.invoke(container, request, response, endpointConfig, pathParams);
	}

}
