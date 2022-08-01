/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.socket.server.support;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
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

	private static final Mono<Map<String, Object>> EMPTY_ATTRIBUTES = Mono.just(Collections.emptyMap());


	private static final boolean tomcatPresent;

	private static final boolean jettyPresent;

	private static final boolean undertowPresent;

	private static final boolean reactorNettyPresent;

	static {
		ClassLoader loader = HandshakeWebSocketService.class.getClassLoader();
		tomcatPresent = ClassUtils.isPresent("org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", loader);
		jettyPresent = ClassUtils.isPresent("org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", loader);
		undertowPresent = ClassUtils.isPresent("io.undertow.websockets.WebSocketProtocolHandshakeHandler", loader);
		reactorNettyPresent = ClassUtils.isPresent("reactor.netty.http.server.HttpServerResponse", loader);
	}


	protected static final Log logger = LogFactory.getLog(HandshakeWebSocketService.class);


	private final RequestUpgradeStrategy upgradeStrategy;

	@Nullable
	private Predicate<String> sessionAttributePredicate;

	private volatile boolean running;


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

	/**
	 * Configure a predicate to use to extract
	 * {@link org.springframework.web.server.WebSession WebSession} attributes
	 * and use them to initialize the WebSocket session with.
	 * <p>By default this is not set in which case no attributes are passed.
	 * @param predicate the predicate
	 * @since 5.1
	 */
	public void setSessionAttributePredicate(@Nullable Predicate<String> predicate) {
		this.sessionAttributePredicate = predicate;
	}

	/**
	 * Return the configured predicate for initialization WebSocket session
	 * attributes from {@code WebSession} attributes.
	 * @since 5.1
	 */
	@Nullable
	public Predicate<String> getSessionAttributePredicate() {
		return this.sessionAttributePredicate;
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

		if (HttpMethod.GET != method) {
			return Mono.error(new MethodNotAllowedException(
					request.getMethod(), Collections.singleton(HttpMethod.GET)));
		}

		if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
			return handleBadRequest(exchange, "Invalid 'Upgrade' header: " + headers);
		}

		List<String> connectionValue = headers.getConnection();
		if (!connectionValue.contains("Upgrade") && !connectionValue.contains("upgrade")) {
			return handleBadRequest(exchange, "Invalid 'Connection' header: " + headers);
		}

		String key = headers.getFirst(SEC_WEBSOCKET_KEY);
		if (key == null) {
			return handleBadRequest(exchange, "Missing \"Sec-WebSocket-Key\" header");
		}

		String protocol = selectProtocol(headers, handler);

		return initAttributes(exchange).flatMap(attributes ->
				this.upgradeStrategy.upgrade(exchange, handler, protocol,
						() -> createHandshakeInfo(exchange, request, protocol, attributes))
		);
	}

	private Mono<Void> handleBadRequest(ServerWebExchange exchange, String reason) {
		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + reason);
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

	private Mono<Map<String, Object>> initAttributes(ServerWebExchange exchange) {
		if (this.sessionAttributePredicate == null) {
			return EMPTY_ATTRIBUTES;
		}
		return exchange.getSession().map(session ->
				session.getAttributes().entrySet().stream()
						.filter(entry -> this.sessionAttributePredicate.test(entry.getKey()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	private HandshakeInfo createHandshakeInfo(ServerWebExchange exchange, ServerHttpRequest request,
			@Nullable String protocol, Map<String, Object> attributes) {

		URI uri = request.getURI();
		// Copy request headers, as they might be pooled and recycled by
		// the server implementation once the handshake HTTP exchange is done.
		HttpHeaders headers = new HttpHeaders();
		headers.addAll(request.getHeaders());
		MultiValueMap<String, HttpCookie> cookies = request.getCookies();
		Mono<Principal> principal = exchange.getPrincipal();
		String logPrefix = exchange.getLogPrefix();
		InetSocketAddress remoteAddress = request.getRemoteAddress();
		return new HandshakeInfo(uri, headers, cookies, principal, protocol, remoteAddress, attributes, logPrefix);
	}

}
