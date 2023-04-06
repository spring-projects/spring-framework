/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.WebLogicRequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.WebSphereRequestUpgradeStrategy;

/**
 * A base class for {@link HandshakeHandler} implementations, independent of the Servlet API.
 *
 * <p>Performs initial validation of the WebSocket handshake request - possibly rejecting it
 * through the appropriate HTTP status code - while also allowing its subclasses to override
 * various parts of the negotiation process (e.g. origin validation, sub-protocol negotiation,
 * extensions negotiation, etc).
 *
 * <p>If the negotiation succeeds, the actual upgrade is delegated to a server-specific
 * {@link org.springframework.web.socket.server.RequestUpgradeStrategy}, which will update
 * the response as necessary and initialize the WebSocket. Currently, supported servers are
 * Jetty 9.0-9.3, Tomcat 7.0.47+ and 8.x, Undertow 1.0-1.3, GlassFish 4.1+, WebLogic 12.1.3+.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2
 * @see org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy
 * @see org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler, Lifecycle {

	private static final boolean tomcatWsPresent;

	private static final boolean jettyWsPresent;

	private static final boolean undertowWsPresent;

	private static final boolean glassfishWsPresent;

	private static final boolean weblogicWsPresent;

	private static final boolean websphereWsPresent;

	static {
		ClassLoader classLoader = AbstractHandshakeHandler.class.getClassLoader();
		tomcatWsPresent = ClassUtils.isPresent(
				"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", classLoader);
		jettyWsPresent = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", classLoader);
		undertowWsPresent = ClassUtils.isPresent(
				"io.undertow.websockets.jsr.ServerWebSocketContainer", classLoader);
		glassfishWsPresent = ClassUtils.isPresent(
				"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", classLoader);
		weblogicWsPresent = ClassUtils.isPresent(
				"weblogic.websocket.tyrus.TyrusServletWriter", classLoader);
		websphereWsPresent = ClassUtils.isPresent(
				"com.ibm.websphere.wsoc.WsWsocServerContainer", classLoader);
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private final RequestUpgradeStrategy requestUpgradeStrategy;

	private final List<String> supportedProtocols = new ArrayList<>();

	private volatile boolean running;


	/**
	 * Default constructor that auto-detects and instantiates a
	 * {@link RequestUpgradeStrategy} suitable for the runtime container.
	 * @throws IllegalStateException if no {@link RequestUpgradeStrategy} can be found.
	 */
	protected AbstractHandshakeHandler() {
		this(initRequestUpgradeStrategy());
	}

	/**
	 * A constructor that accepts a runtime-specific {@link RequestUpgradeStrategy}.
	 * @param requestUpgradeStrategy the upgrade strategy to use
	 */
	protected AbstractHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		Assert.notNull(requestUpgradeStrategy, "RequestUpgradeStrategy must not be null");
		this.requestUpgradeStrategy = requestUpgradeStrategy;
	}


	/**
	 * Return the {@link RequestUpgradeStrategy} for WebSocket requests.
	 */
	public RequestUpgradeStrategy getRequestUpgradeStrategy() {
		return this.requestUpgradeStrategy;
	}

	/**
	 * Use this property to configure the list of supported sub-protocols.
	 * The first configured sub-protocol that matches a client-requested sub-protocol
	 * is accepted. If there are no matches the response will not contain a
	 * {@literal Sec-WebSocket-Protocol} header.
	 * <p>Note that if the WebSocketHandler passed in at runtime is an instance of
	 * {@link SubProtocolCapable} then there is no need to explicitly configure
	 * this property. That is certainly the case with the built-in STOMP over
	 * WebSocket support. Therefore, this property should be configured explicitly
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
		return StringUtils.toStringArray(this.supportedProtocols);
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
		if (this.requestUpgradeStrategy instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			doStop();
		}
	}

	protected void doStop() {
		if (this.requestUpgradeStrategy instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Processing request " + request.getURI() + " with headers=" + headers);
		}
		try {
			if (HttpMethod.GET != request.getMethod()) {
				response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
				response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
				if (logger.isErrorEnabled()) {
					logger.error("Handshake failed due to unexpected HTTP method: " + request.getMethod());
				}
				return false;
			}
			if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
				handleInvalidUpgradeHeader(request, response);
				return false;
			}
			if (!headers.getConnection().contains("Upgrade") && !headers.getConnection().contains("upgrade")) {
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
				if (logger.isErrorEnabled()) {
					logger.error("Missing \"Sec-WebSocket-Key\" header");
				}
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return false;
			}
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		}

		String subProtocol = selectProtocol(headers.getSecWebSocketProtocol(), wsHandler);
		List<WebSocketExtension> requested = headers.getSecWebSocketExtensions();
		List<WebSocketExtension> supported = this.requestUpgradeStrategy.getSupportedExtensions(request);
		List<WebSocketExtension> extensions = filterRequestedExtensions(request, requested, supported);
		Principal user = determineUser(request, wsHandler, attributes);

		if (logger.isTraceEnabled()) {
			logger.trace("Upgrading to WebSocket, subProtocol=" + subProtocol + ", extensions=" + extensions);
		}
		this.requestUpgradeStrategy.upgrade(request, response, subProtocol, extensions, user, wsHandler, attributes);
		return true;
	}

	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to invalid Upgrade header: " + request.getHeaders().getUpgrade(), -1, true));
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"WebSocket\".".getBytes(StandardCharsets.UTF_8));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to invalid Connection header" + request.getHeaders().getConnection(), -1, true));
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes(StandardCharsets.UTF_8));
	}

	protected boolean isWebSocketVersionSupported(WebSocketHttpHeaders httpHeaders) {
		String version = httpHeaders.getSecWebSocketVersion();
		String[] supportedVersions = getSupportedVersions();
		for (String supportedVersion : supportedVersions) {
			if (supportedVersion.trim().equals(version)) {
				return true;
			}
		}
		return false;
	}

	protected String[] getSupportedVersions() {
		return this.requestUpgradeStrategy.getSupportedVersions();
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		if (logger.isErrorEnabled()) {
			String version = request.getHeaders().getFirst("Sec-WebSocket-Version");
			logger.error(LogFormatUtils.formatValue(
					"Handshake failed due to unsupported WebSocket version: " + version +
							". Supported versions: " + Arrays.toString(getSupportedVersions()), -1, true));
		}
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().set(WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION,
				StringUtils.arrayToCommaDelimitedString(getSupportedVersions()));
	}

	/**
	 * Return whether the request {@code Origin} header value is valid or not.
	 * By default, all origins as considered as valid. Consider using an
	 * {@link OriginHandshakeInterceptor} for filtering origins if needed.
	 */
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
	 * @see #determineHandlerSupportedProtocols(WebSocketHandler)
	 */
	@Nullable
	protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
		List<String> handlerProtocols = determineHandlerSupportedProtocols(webSocketHandler);
		for (String protocol : requestedProtocols) {
			if (handlerProtocols.contains(protocol.toLowerCase())) {
				return protocol;
			}
			if (this.supportedProtocols.contains(protocol.toLowerCase())) {
				return protocol;
			}
		}
		return null;
	}

	/**
	 * Determine the sub-protocols supported by the given WebSocketHandler by
	 * checking whether it is an instance of {@link SubProtocolCapable}.
	 * @param handler the handler to check
	 * @return a list of supported protocols, or an empty list if none available
	 */
	protected final List<String> determineHandlerSupportedProtocols(WebSocketHandler handler) {
		WebSocketHandler handlerToCheck = WebSocketHandlerDecorator.unwrap(handler);
		List<String> subProtocols = null;
		if (handlerToCheck instanceof SubProtocolCapable subProtocolCapable) {
			subProtocols = subProtocolCapable.getSubProtocols();
		}
		return (subProtocols != null ? subProtocols : Collections.emptyList());
	}

	/**
	 * Filter the list of requested WebSocket extensions.
	 * <p>As of 4.1, the default implementation of this method filters the list to
	 * leave only extensions that are both requested and supported.
	 * @param request the current request
	 * @param requestedExtensions the list of extensions requested by the client
	 * @param supportedExtensions the list of extensions supported by the server
	 * @return the selected extensions or an empty list
	 */
	protected List<WebSocketExtension> filterRequestedExtensions(ServerHttpRequest request,
			List<WebSocketExtension> requestedExtensions, List<WebSocketExtension> supportedExtensions) {

		List<WebSocketExtension> result = new ArrayList<>(requestedExtensions.size());
		for (WebSocketExtension extension : requestedExtensions) {
			if (supportedExtensions.contains(extension)) {
				result.add(extension);
			}
		}
		return result;
	}

	/**
	 * A method that can be used to associate a user with the WebSocket session
	 * in the process of being established. The default implementation calls
	 * {@link ServerHttpRequest#getPrincipal()}
	 * <p>Subclasses can provide custom logic for associating a user with a session,
	 * for example for assigning a name to anonymous users (i.e. not fully authenticated).
	 * @param request the handshake request
	 * @param wsHandler the WebSocket handler that will handle messages
	 * @param attributes handshake attributes to pass to the WebSocket session
	 * @return the user for the WebSocket session, or {@code null} if not available
	 */
	@Nullable
	protected Principal determineUser(
			ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

		return request.getPrincipal();
	}


	private static RequestUpgradeStrategy initRequestUpgradeStrategy() {
		if (tomcatWsPresent) {
			return new TomcatRequestUpgradeStrategy();
		}
		else if (jettyWsPresent) {
			return new JettyRequestUpgradeStrategy();
		}
		else if (undertowWsPresent) {
			return new UndertowRequestUpgradeStrategy();
		}
		else if (glassfishWsPresent) {
			return TyrusStrategyDelegate.forGlassFish();
		}
		else if (weblogicWsPresent) {
			return TyrusStrategyDelegate.forWebLogic();
		}
		else if (websphereWsPresent) {
			return new WebSphereRequestUpgradeStrategy();
		}
		else {
			// Let's assume Jakarta WebSocket API 2.1+
			return new StandardWebSocketUpgradeStrategy();
		}
	}


	/**
	 * Inner class to avoid a reachable dependency on Tyrus API.
	 */
	private static class TyrusStrategyDelegate {

		public static RequestUpgradeStrategy forGlassFish() {
			return new GlassFishRequestUpgradeStrategy();
		}

		public static RequestUpgradeStrategy forWebLogic() {
			return new WebLogicRequestUpgradeStrategy();
		}
	}

}
