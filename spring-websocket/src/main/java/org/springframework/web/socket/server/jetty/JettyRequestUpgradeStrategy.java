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

package org.springframework.web.socket.server.jetty;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A {@link RequestUpgradeStrategy} for Jetty 11.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String[] SUPPORTED_VERSIONS = new String[] {"13"};


	@Override
	public String[] getSupportedVersions() {
		return SUPPORTED_VERSIONS;
	}

	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		return Collections.emptyList();
	}


	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			@Nullable Principal user, WebSocketHandler handler, Map<String, Object> attributes)
			throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
		ServletContext servletContext = servletRequest.getServletContext();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(handler, session);

		JettyWebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse) -> {
			if (selectedProtocol != null) {
				upgradeResponse.setAcceptedSubProtocol(selectedProtocol);
			}
			return handlerAdapter;
		};

		JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);

		try {
			container.upgrade(webSocketCreator, servletRequest, servletResponse);
		}
		catch (UndeclaredThrowableException ex) {
			throw new HandshakeFailureException("Failed to upgrade", ex.getUndeclaredThrowable());
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Failed to upgrade", ex);
		}
	}


}
