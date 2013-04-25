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

package org.springframework.websocket.server.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.apache.tomcat.websocket.server.WsHttpUpgradeHandler;
import org.apache.tomcat.websocket.server.WsServerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.websocket.server.endpoint.EndpointRegistration;

/**
 * Tomcat support for upgrading an {@link HttpServletRequest} during a WebSocket handshake.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TomcatRequestUpgradeStrategy extends AbstractEndpointUpgradeStrategy {

	@Override
	public String[] getSupportedVersions() {
		return new String[] { "13" };
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, Endpoint endpoint) throws IOException {

		Assert.isTrue(request instanceof ServletServerHttpRequest);
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		WsHttpUpgradeHandler upgradeHandler = servletRequest.upgrade(WsHttpUpgradeHandler.class);

		WsHandshakeRequest webSocketRequest = new WsHandshakeRequest(servletRequest);
		try {
			Method method = ReflectionUtils.findMethod(WsHandshakeRequest.class, "finished");
			ReflectionUtils.makeAccessible(method);
			method.invoke(webSocketRequest);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to upgrade HttpServletRequest", ex);
		}

		// TODO: use ServletContext attribute when Tomcat is updated
		WsServerContainer serverContainer = WsServerContainer.getServerContainer();

		ServerEndpointConfig endpointConfig = new EndpointRegistration("/shouldntmatter", endpoint);

		upgradeHandler.preInit(endpoint, endpointConfig, serverContainer, webSocketRequest,
				selectedProtocol, Collections.<String, String> emptyMap(), servletRequest.isSecure());
	}

}
