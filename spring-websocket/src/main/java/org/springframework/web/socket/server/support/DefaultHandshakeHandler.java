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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.support.SubProtocolCapable;
import org.springframework.web.socket.support.WebSocketExtension;
import org.springframework.web.socket.support.WebSocketHandlerDecorator;
import org.springframework.web.socket.support.WebSocketHttpHeaders;

/**
 * A default {@link org.springframework.web.socket.server.HandshakeHandler} implementation. Performs initial validation of the
 * WebSocket handshake request -- possibly rejecting it through the appropriate HTTP
 * status code -- while also allowing sub-classes to override various parts of the
 * negotiation process (e.g. origin validation, sub-protocol negotiation,
 * extensions negotiation, etc).
 *
 * <p>If the negotiation succeeds, the actual upgrade is delegated to a server-specific
 * {@link org.springframework.web.socket.server.RequestUpgradeStrategy}, which will update the response as necessary and
 * initialize the WebSocket. Currently supported servers are Tomcat 7 and 8, Jetty 9, and
 * Glassfish 4.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultHandshakeHandler implements HandshakeHandler {

	protected Log logger = LogFactory.getLog(getClass());

	private static final boolean tomcatWsPresent = ClassUtils.isPresent(
			"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", HandshakeHandler.class.getClassLoader());

	private static final boolean jettyWsPresent = ClassUtils.isPresent(
			"org.eclipse.jetty.websocket.server.WebSocketServerFactory", HandshakeHandler.class.getClassLoader());

	private static final boolean glassFishWsPresent = ClassUtils.isPresent(
			"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", HandshakeHandler.class.getClassLoader());

	private static final boolean glassFish40WsPresent = ClassUtils.isPresent(
			"org.glassfish.tyrus.server.TyrusEndpoint", HandshakeHandler.class.getClassLoader());


	private final RequestUpgradeStrategy requestUpgradeStrategy;

	private final List<String> supportedProtocols = new ArrayList<String>();


	/**
	 * Default constructor that auto-detects and instantiates a
	 * {@link RequestUpgradeStrategy} suitable for the runtime container.
	 * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
	 */
	public DefaultHandshakeHandler() {
		this(initRequestUpgradeStrategy());
	}

	private static RequestUpgradeStrategy initRequestUpgradeStrategy() {
		String className;
		if (tomcatWsPresent) {
			className = "org.springframework.web.socket.server.support.TomcatRequestUpgradeStrategy";
		}
		else if (jettyWsPresent) {
			className = "org.springframework.web.socket.server.support.JettyRequestUpgradeStrategy";
		}
		else if (glassFishWsPresent) {
			if (glassFish40WsPresent) {
				className = "org.springframework.web.socket.server.support.GlassFish40RequestUpgradeStrategy";
			}
			else {
				className = "org.springframework.web.socket.server.support.GlassFishRequestUpgradeStrategy";
			}
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

	/**
	 * A constructor that accepts a runtime specific {@link RequestUpgradeStrategy}.
	 * @param upgradeStrategy the upgrade strategy
	 */
	public DefaultHandshakeHandler(RequestUpgradeStrategy upgradeStrategy) {
		this.requestUpgradeStrategy = upgradeStrategy;
	}


	/**
	 * Use this property to configure the list of supported sub-protocols.
	 * The first configured sub-protocol that matches a client-requested sub-protocol
	 * is accepted. If there are no matches the response will not contain a
	 * {@literal Sec-WebSocket-Protocol} header.
	 * <p>Note that if the WebSocketHandler passed in at runtime is an instance of
	 * {@link SubProtocolCapable} then there is not need to explicitly configure
	 * this property. That is certainly the case with the built-in STOMP over
	 * WebSocket support. Therefore this property should be configured explicitly
	 * only if the WebSocketHandler does not implement {@code SubProtocolCapable}.
	 */
	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols.clear();
		for (String protocol : protocols) {
			this.supportedProtocols.add(protocol.toLowerCase());
		}
	}

	/**
	 * Return the list of supported sub-protocols.
	 */
	public String[] getSupportedProtocols() {
		return this.supportedProtocols.toArray(new String[this.supportedProtocols.size()]);
	}

	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());

		if (logger.isDebugEnabled()) {
			logger.debug("Initiating handshake for " + request.getURI() + ", headers=" + headers);
		}

		try {
			if (!HttpMethod.GET.equals(request.getMethod())) {
				response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
				response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
				logger.debug("Only HTTP GET is allowed, current method is " + request.getMethod());
				return false;
			}
			if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
				handleInvalidUpgradeHeader(request, response);
				return false;
			}
			if (!headers.getConnection().contains("Upgrade") &&
					!headers.getConnection().contains("upgrade")) {
				handleInvalidConnectHeader(request, response);
				return false;
			}
			if (!isWebSocketVersionSupported(headers)) {
				handleWebSocketVersionNotSupported(request, response);
				return false;
			}
			if (!isValidOrigin(request)) {
				response.setStatusCode(HttpStatus.FORBIDDEN);
				return false;
			}
			String wsKey = headers.getSecWebSocketKey();
			if (wsKey == null) {
				logger.debug("Missing \"Sec-WebSocket-Key\" header");
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return false;
			}
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket, uri=" + request.getURI(), ex);
		}

		String subProtocol = selectProtocol(headers.getSecWebSocketProtocol(), wsHandler);
		if (logger.isDebugEnabled()) {
			logger.debug("Selected sub-protocol: '" + subProtocol + "'");
		}

		List<WebSocketExtension> requested = headers.getSecWebSocketExtensions();
		List<WebSocketExtension> supported = this.requestUpgradeStrategy.getSupportedExtensions(request);
		List<WebSocketExtension> extensions = filterRequestedExtensions(request, requested, supported);

		if (logger.isDebugEnabled()) {
			logger.debug("Upgrading request, sub-protocol=" + subProtocol + ", extensions=" + extensions);
		}

		this.requestUpgradeStrategy.upgrade(request, response, subProtocol, extensions, wsHandler, attributes);

		return true;
	}

	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Upgrade header " + request.getHeaders().getUpgrade());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"WebSocket\".".getBytes("UTF-8"));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		logger.debug("Invalid Connection header " + request.getHeaders().getConnection());
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes("UTF-8"));
	}

	protected boolean isWebSocketVersionSupported(WebSocketHttpHeaders httpHeaders) {
		String version = httpHeaders.getSecWebSocketVersion();
		String[] supportedVersions = getSupportedVerions();
		if (logger.isDebugEnabled()) {
			logger.debug("Requested version=" + version + ", supported=" + Arrays.toString(supportedVersions));
		}
		for (String supportedVersion : supportedVersions) {
			if (supportedVersion.trim().equals(version)) {
				return true;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Version=" + version + " is not a supported version");
		}
		return false;
	}

	protected String[] getSupportedVerions() {
		return this.requestUpgradeStrategy.getSupportedVersions();
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		logger.debug("WebSocket version not supported " + request.getHeaders().get("Sec-WebSocket-Version"));
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().put(WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION, Arrays.asList(
				StringUtils.arrayToCommaDelimitedString(getSupportedVerions())));
	}

	protected boolean isValidOrigin(ServerHttpRequest request) {
		return true;
	}

	/**
	 * Perform the sub-protocol negotiation based on requested and supported sub-protocols.
	 * For the list of supported sub-protocols, this method first checks if the target
	 * WebSocketHandler is a {@link SubProtocolCapable} and then also checks if any
	 * sub-protocols have been explicitly configured with
	 * {@link #setSupportedProtocols(String...)}.
	 * @param requestedProtocols the requested sub-protocols
	 * @param webSocketHandler the WebSocketHandler that will be used
	 * @return the selected protocols or {@code null}
	 * @see #determineHandlerSupportedProtocols(org.springframework.web.socket.WebSocketHandler)
	 */
	protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
		if (requestedProtocols != null) {
			List<String> handlerProtocols = determineHandlerSupportedProtocols(webSocketHandler);
			if (logger.isDebugEnabled()) {
				logger.debug("Requested sub-protocol(s): " + requestedProtocols +
						", WebSocketHandler supported sub-protocol(s): " + handlerProtocols +
						", configured sub-protocol(s): " + this.supportedProtocols);
			}
			for (String protocol : requestedProtocols) {
				if (handlerProtocols.contains(protocol.toLowerCase())) {
					return protocol;
				}
				if (this.supportedProtocols.contains(protocol.toLowerCase())) {
					return protocol;
				}
			}
		}
		return null;
	}

	/**
	 * Determine the sub-protocols supported by the given WebSocketHandler by checking
	 * whether it is an instance of {@link SubProtocolCapable}.
	 * @param handler the handler to check
	 * @return a list of supported protocols or an empty list
	 */
	protected final List<String> determineHandlerSupportedProtocols(WebSocketHandler handler) {
		List<String> subProtocols = null;
		if (handler instanceof SubProtocolCapable) {
			subProtocols = ((SubProtocolCapable) handler).getSubProtocols();
		}
		else if (handler instanceof WebSocketHandlerDecorator) {
			WebSocketHandler lastHandler = ((WebSocketHandlerDecorator) handler).getLastHandler();
			if (lastHandler instanceof SubProtocolCapable) {
				subProtocols = ((SubProtocolCapable) lastHandler).getSubProtocols();;
			}
		}
		return (subProtocols != null) ? subProtocols : Collections.<String>emptyList();
	}

	/**
	 * Filter the list of requested WebSocket extensions.
	 * <p>By default all request extensions are returned. The WebSocket server will further
	 * compare the requested extensions against the list of supported extensions and
	 * return only the ones that are both requested and supported.
	 * @param request the current request
	 * @param requested the list of extensions requested by the client
	 * @param supported the list of extensions supported by the server
	 * @return the selected extensions or an empty list
	 */
	protected List<WebSocketExtension> filterRequestedExtensions(ServerHttpRequest request,
			List<WebSocketExtension> requested, List<WebSocketExtension> supported) {

		if (requested != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Requested extension(s): " + requested + ", supported extension(s): " + supported);
			}
		}
		return requested;
	}
}
