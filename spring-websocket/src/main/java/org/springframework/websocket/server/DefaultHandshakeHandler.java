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

package org.springframework.websocket.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.websocket.WebSocketHandler;

/**
 * TODO
 * <p>
 * A container-specific {@link RequestUpgradeStrategy} is required since standard Java
 * WebSocket currently does not provide a way to initiate a WebSocket handshake.
 * Currently available are implementations for Tomcat and Glassfish.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultHandshakeHandler implements HandshakeHandler {

	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	protected Log logger = LogFactory.getLog(getClass());

	private List<String> supportedProtocols = new ArrayList<String>();

	private RequestUpgradeStrategy requestUpgradeStrategy;


	/**
	 * Default constructor that auto-detects and instantiates a
	 * {@link RequestUpgradeStrategy} suitable for the runtime container.
	 *
	 * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
	 */
	public DefaultHandshakeHandler() {
		this.requestUpgradeStrategy = new RequestUpgradeStrategyFactory().create();
	}

	/**
	 * A constructor that accepts a runtime specific {@link RequestUpgradeStrategy}.
	 * @param upgradeStrategy the upgrade strategy
	 */
	public DefaultHandshakeHandler(RequestUpgradeStrategy upgradeStrategy) {
		this.requestUpgradeStrategy = upgradeStrategy;
	}


	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols = Arrays.asList(protocols);
	}

	public String[] getSupportedProtocols() {
		return this.supportedProtocols.toArray(new String[this.supportedProtocols.size()]);
	}

	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler) throws IOException, HandshakeFailureException {

		logger.debug("Starting handshake for " + request.getURI());

		if (!HttpMethod.GET.equals(request.getMethod())) {
			response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
			response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
			logger.debug("Only HTTP GET is allowed, current method is " + request.getMethod());
			return false;
		}
		if (!"WebSocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
			handleInvalidUpgradeHeader(request, response);
			return false;
		}
		if (!request.getHeaders().getConnection().contains("Upgrade") &&
				!request.getHeaders().getConnection().contains("upgrade")) {
			handleInvalidConnectHeader(request, response);
			return false;
		}
		if (!isWebSocketVersionSupported(request)) {
			handleWebSocketVersionNotSupported(request, response);
			return false;
		}
		if (!isValidOrigin(request)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}
		String wsKey = request.getHeaders().getSecWebSocketKey();
		if (wsKey == null) {
			logger.debug("Missing \"Sec-WebSocket-Key\" header");
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			return false;
		}

		String selectedProtocol = selectProtocol(request.getHeaders().getSecWebSocketProtocol());
		// TODO: select extensions

		logger.debug("Upgrading HTTP request");

		response.setStatusCode(HttpStatus.SWITCHING_PROTOCOLS);
		response.getHeaders().setUpgrade("WebSocket");
		response.getHeaders().setConnection("Upgrade");
		response.getHeaders().setSecWebSocketProtocol(selectedProtocol);
		response.getHeaders().setSecWebSocketAccept(getWebSocketKeyHash(wsKey));
		// TODO: response.getHeaders().setSecWebSocketExtensions(extensions);

		response.flush();

		if (logger.isTraceEnabled()) {
			logger.trace("Upgrading with " + webSocketHandler);
		}

		this.requestUpgradeStrategy.upgrade(request, response, selectedProtocol, webSocketHandler);

		return true;
	}

	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Upgrade header " + request.getHeaders().getUpgrade());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"websocket\".".getBytes("UTF-8"));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Connection header " + request.getHeaders().getConnection());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes("UTF-8"));
	}

	protected boolean isWebSocketVersionSupported(ServerHttpRequest request) {
		String requestedVersion = request.getHeaders().getSecWebSocketVersion();
		for (String supportedVersion : getSupportedVerions()) {
			if (supportedVersion.equals(requestedVersion)) {
				return true;
			}
		}
		return false;
	}

	protected String[] getSupportedVerions() {
		return this.requestUpgradeStrategy.getSupportedVersions();
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		logger.debug("WebSocket version not supported " + request.getHeaders().get("Sec-WebSocket-Version"));
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().setSecWebSocketVersion(StringUtils.arrayToCommaDelimitedString(getSupportedVerions()));
	}

	protected boolean isValidOrigin(ServerHttpRequest request) {
		String origin = request.getHeaders().getOrigin();
		if (origin != null) {
			// UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(origin);
			// TODO
			// A simple strategy checks against the current request's scheme/port/host
			// Or match scheme, port, and host against configured allowed origins (wild cards for hosts?)
			// return false;
		}
		return true;
	}

	protected String selectProtocol(List<String> requestedProtocols) {
		if (CollectionUtils.isEmpty(requestedProtocols)) {
			for (String protocol : requestedProtocols) {
				if (this.supportedProtocols.contains(protocol)) {
					return protocol;
				}
			}
		}
		return null;
	}

    private String getWebSocketKeyHash(String key) throws HandshakeFailureException {
    	try {
    		MessageDigest digest = MessageDigest.getInstance("SHA1");
    		byte[] bytes = digest.digest((key + GUID).getBytes(Charset.forName("ISO-8859-1")));
    		return DatatypeConverter.printBase64Binary(bytes);
    	}
    	catch (NoSuchAlgorithmException ex) {
    		throw new HandshakeFailureException("Failed to generate value for Sec-WebSocket-Key header", ex);
    	}
    }


    private static class RequestUpgradeStrategyFactory {

		private static final boolean tomcatWebSocketPresent = ClassUtils.isPresent(
				"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", DefaultHandshakeHandler.class.getClassLoader());

		private static final boolean glassfishWebSocketPresent = ClassUtils.isPresent(
				"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", DefaultHandshakeHandler.class.getClassLoader());

		private static final boolean jettyWebSocketPresent = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.server.UpgradeContext", DefaultHandshakeHandler.class.getClassLoader());

		private RequestUpgradeStrategy create() {
			String className;
			if (tomcatWebSocketPresent) {
				className = "org.springframework.websocket.server.support.TomcatRequestUpgradeStrategy";
			}
			else if (glassfishWebSocketPresent) {
				className = "org.springframework.websocket.server.support.GlassfishRequestUpgradeStrategy";
			}
			else if (jettyWebSocketPresent) {
				className = "org.springframework.websocket.server.support.JettyRequestUpgradeStrategy";
			}
			else {
				throw new IllegalStateException("No suitable " + RequestUpgradeStrategy.class.getSimpleName());
			}
			try {
				Class<?> clazz = ClassUtils.forName(className, DefaultHandshakeHandler.class.getClassLoader());
				return (RequestUpgradeStrategy) BeanUtils.instantiateClass(clazz.getConstructor());
			}
			catch (Throwable t) {
				throw new IllegalStateException("Failed to instantiate " + className, t);
			}
		}
	}

}
