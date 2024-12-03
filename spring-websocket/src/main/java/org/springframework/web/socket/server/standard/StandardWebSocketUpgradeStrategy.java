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

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Extension;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.standard.StandardToWebSocketExtensionAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for the Jakarta WebSocket API 2.1+.
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
public class StandardWebSocketUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String[] SUPPORTED_VERSIONS = new String[] {"13"};

	@Nullable
	private volatile List<WebSocketExtension> extensions;


	@Override
	public String[] getSupportedVersions() {
		return SUPPORTED_VERSIONS;
	}

	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		List<WebSocketExtension> extensions = this.extensions;
		if (extensions == null) {
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			extensions = getInstalledExtensions(getContainer(servletRequest));
			this.extensions = extensions;
		}
		return extensions;
	}

	protected List<WebSocketExtension> getInstalledExtensions(WebSocketContainer container) {
		List<WebSocketExtension> result = new ArrayList<>();
		for (Extension extension : container.getInstalledExtensions()) {
			result.add(new StandardToWebSocketExtensionAdapter(extension));
		}
		return result;
	}


	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			@Nullable Principal user, WebSocketHandler wsHandler, Map<String, Object> attrs)
			throws HandshakeFailureException {

		HttpHeaders headers = request.getHeaders();
		InetSocketAddress localAddr = null;
		try {
			localAddr = request.getLocalAddress();
		}
		catch (Exception ex) {
			// Ignore
		}
		InetSocketAddress remoteAddr = null;
		try {
			remoteAddr = request.getRemoteAddress();
		}
		catch (Exception ex) {
			// Ignore
		}

		StandardWebSocketSession session = new StandardWebSocketSession(headers, attrs, localAddr, remoteAddr, user);
		StandardWebSocketHandlerAdapter endpoint = new StandardWebSocketHandlerAdapter(wsHandler, session);

		List<Extension> extensions = new ArrayList<>();
		for (WebSocketExtension extension : selectedExtensions) {
			extensions.add(new WebSocketToStandardExtensionAdapter(extension));
		}

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		StringBuffer requestUrl = servletRequest.getRequestURL();
		String path = servletRequest.getRequestURI();  // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
		endpointConfig.setExtensions(extensions);

		try {
			getContainer(servletRequest).upgradeHttpToWebSocket(
					servletRequest, servletResponse, endpointConfig, pathParams);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Servlet request failed to upgrade to WebSocket: " + requestUrl, ex);
		}
	}


	protected ServerContainer getContainer(HttpServletRequest request) {
		ServletContext servletContext = request.getServletContext();
		String attrName = "jakarta.websocket.server.ServerContainer";
		ServerContainer container = (ServerContainer) servletContext.getAttribute(attrName);
		Assert.notNull(container, "No 'jakarta.websocket.server.ServerContainer' ServletContext attribute. " +
				"Are you running in a Servlet container that supports JSR-356?");
		return container;
	}

	protected final HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
		Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
		return ((ServletServerHttpRequest) request).getServletRequest();
	}

	protected final HttpServletResponse getHttpServletResponse(ServerHttpResponse response) {
		Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
		return ((ServletServerHttpResponse) response).getServletResponse();
	}

}
