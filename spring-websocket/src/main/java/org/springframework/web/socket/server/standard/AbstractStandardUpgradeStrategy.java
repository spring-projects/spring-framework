/*
 * Copyright 2002-2017 the original author or authors.
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

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
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
 * A base class for {@link RequestUpgradeStrategy} implementations that build
 * on the standard WebSocket API for Java (JSR-356).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractStandardUpgradeStrategy implements RequestUpgradeStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile List<WebSocketExtension> extensions;


	protected ServerContainer getContainer(HttpServletRequest request) {
		ServletContext servletContext = request.getServletContext();
		String attrName = "javax.websocket.server.ServerContainer";
		ServerContainer container = (ServerContainer) servletContext.getAttribute(attrName);
		Assert.notNull(container, "No 'javax.websocket.server.ServerContainer' ServletContext attribute. " +
				"Are you running in a Servlet container that supports JSR-356?");
		return container;
	}

	protected final HttpServletRequest getHttpServletRequest(ServerHttpRequest request) {
		Assert.isTrue(request instanceof ServletServerHttpRequest, "ServletServerHttpRequest required");
		return ((ServletServerHttpRequest) request).getServletRequest();
	}

	protected final HttpServletResponse getHttpServletResponse(ServerHttpResponse response) {
		Assert.isTrue(response instanceof ServletServerHttpResponse, "ServletServerHttpResponse required");
		return ((ServletServerHttpResponse) response).getServletResponse();
	}


	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		if (this.extensions == null) {
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			this.extensions = getInstalledExtensions(getContainer(servletRequest));
		}
		return this.extensions;
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
			String selectedProtocol, List<WebSocketExtension> selectedExtensions, Principal user,
			WebSocketHandler wsHandler, Map<String, Object> attrs) throws HandshakeFailureException {

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

		upgradeInternal(request, response, selectedProtocol, extensions, endpoint);
	}

	protected abstract void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException;

}
