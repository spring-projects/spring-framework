/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.socket.server.support;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * {@code WebSocketService} implementation that handles a WebSocket HTTP
 * handshake request by delegating to a {@link RequestUpgradeStrategy} which
 * is either auto-detected (no-arg constructor) from the classpath but can
 * also be explicitly configured.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HandshakeWebSocketService implements WebSocketService, Lifecycle {

	private static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

	private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";


	private static final boolean tomcatPresent = ClassUtils.isPresent(
			"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler",
			HandshakeWebSocketService.class.getClassLoader());

	private static final boolean jettyPresent = ClassUtils.isPresent(
			"org.eclipse.jetty.websocket.server.WebSocketServerFactory",
			HandshakeWebSocketService.class.getClassLoader());

	private static final boolean undertowPresent = ClassUtils.isPresent(
			"io.undertow.websockets.WebSocketProtocolHandshakeHandler",
			HandshakeWebSocketService.class.getClassLoader());

	private static final boolean reactorNettyPresent = ClassUtils.isPresent(
			"reactor.ipc.netty.http.server.HttpServerResponse",
			HandshakeWebSocketService.class.getClassLoader());


	protected static final Log logger = LogFactory.getLog(HandshakeWebSocketService.class);


	private final RequestUpgradeStrategy upgradeStrategy;

	private volatile boolean running = false;


	/**
	 * Default constructor automatic, classpath detection based discovery of the
	 * {@link RequestUpgradeStrategy} to use.
	 */
	public HandshakeWebSocketService() {
		this(initUpgradeStrategy());
	}

	/**
	 * Alternative constructor with the {@link RequestUpgradeStrategy} to use.
	 * @param upgradeStrategy the strategy to use
	 */
	public HandshakeWebSocketService(RequestUpgradeStrategy upgradeStrategy) {
		Assert.notNull(upgradeStrategy, "RequestUpgradeStrategy is required");
		this.upgradeStrategy = upgradeStrategy;
	}

	private static RequestUpgradeStrategy initUpgradeStrategy() {
		String className;
		if (tomcatPresent) {
			className = "TomcatRequestUpgradeStrategy";
		}
		else if (jettyPresent) {
			className = "JettyRequestUpgradeStrategy";
		}
		else if (undertowPresent) {
			className = "UndertowRequestUpgradeStrategy";
		}
		else if (reactorNettyPresent) {
			// As late as possible (Reactor Netty commonly used for WebClient)
			className = "ReactorNettyRequestUpgradeStrategy";
		}
		else {
			throw new IllegalStateException("No suitable default RequestUpgradeStrategy found");
		}

		try {
			className = "org.springframework.web.reactive.socket.server.upgrade." + className;
			Class<?> clazz = ClassUtils.forName(className, HandshakeWebSocketService.class.getClassLoader());
			return (RequestUpgradeStrategy) ReflectionUtils.accessibleConstructor(clazz).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Failed to instantiate RequestUpgradeStrategy: " + className, ex);
		}
	}


	/**
	 * Return the {@link RequestUpgradeStrategy} for WebSocket requests.
	 */
	public RequestUpgradeStrategy getUpgradeStrategy() {
		return this.upgradeStrategy;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
		if (getUpgradeStrategy() instanceof Lifecycle) {
			((Lifecycle) getUpgradeStrategy()).start();
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
		if (getUpgradeStrategy() instanceof Lifecycle) {
			((Lifecycle) getUpgradeStrategy()).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		HttpHeaders headers = request.getHeaders();

		if (logger.isDebugEnabled()) {
			logger.debug("Handling " + request.getURI() + " with headers: " + headers);
		}

		if (HttpMethod.GET != method) {
			return Mono.error(new MethodNotAllowedException(
					request.getMethodValue(), Collections.singleton(HttpMethod.GET)));
		}

		if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
			return handleBadRequest("Invalid 'Upgrade' header: " + headers);
		}

		List<String> connectionValue = headers.getConnection();
		if (!connectionValue.contains("Upgrade") && !connectionValue.contains("upgrade")) {
			return handleBadRequest("Invalid 'Connection' header: " + headers);
		}

		String key = headers.getFirst(SEC_WEBSOCKET_KEY);
		if (key == null) {
			return handleBadRequest("Missing \"Sec-WebSocket-Key\" header");
		}

		String protocol = selectProtocol(headers, handler);
		return this.upgradeStrategy.upgrade(exchange, handler, protocol);
	}

	private Mono<Void> handleBadRequest(String reason) {
		if (logger.isDebugEnabled()) {
			logger.debug(reason);
		}
		return Mono.error(new ServerWebInputException(reason));
	}

	@Nullable
	private String selectProtocol(HttpHeaders headers, WebSocketHandler handler) {
		String protocolHeader = headers.getFirst(SEC_WEBSOCKET_PROTOCOL);
		if (protocolHeader != null) {
			List<String> supportedProtocols = handler.getSubProtocols();
			for (String protocol : StringUtils.commaDelimitedListToStringArray(protocolHeader)) {
				if (supportedProtocols.contains(protocol)) {
					return protocol;
				}
			}
		}
		return null;
	}

}
