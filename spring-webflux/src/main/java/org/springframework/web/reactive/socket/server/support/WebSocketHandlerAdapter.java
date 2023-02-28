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

package org.springframework.web.reactive.socket.server.support;

import reactor.core.publisher.Mono;

import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerAdapter} that allows
 * {@link org.springframework.web.reactive.DispatcherHandler} to support
 * handlers of type {@link WebSocketHandler} with such handlers mapped to
 * URL patterns via
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping}.
 *
 * <p>Requests are handled by delegating to a
 * {@link WebSocketService}, by default {@link HandshakeWebSocketService},
 * which checks the WebSocket handshake request parameters, upgrades to a
 * WebSocket interaction, and uses the {@link WebSocketHandler} to handle it.
 *
 * <p>As of 5.3 the WebFlux Java configuration, imported via
 * {@code @EnableWebFlux}, includes a declaration of this adapter and therefore
 * it no longer needs to be present in application configuration.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@ImportRuntimeHints(HandshakeWebSocketServiceRuntimeHints.class)
public class WebSocketHandlerAdapter implements HandlerAdapter, Ordered {

	private final WebSocketService webSocketService;

	private int order = 2;


	/**
	 * Default constructor that creates and uses a
	 * {@link HandshakeWebSocketService}.
	 */
	public WebSocketHandlerAdapter() {
		this(new HandshakeWebSocketService());
	}

	/**
	 * Alternative constructor with the {@link WebSocketService} to use.
	 */
	public WebSocketHandlerAdapter(WebSocketService webSocketService) {
		Assert.notNull(webSocketService, "'webSocketService' is required");
		this.webSocketService = webSocketService;
	}


	/**
	 * Set the order value for this adapter.
	 * <p>By default this is set to 2.
	 * @param order the value to set to
	 * @since 5.3
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Return the {@link #setOrder(int) configured} order for this instance.
	 * @since 5.3
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Return the configured {@code WebSocketService} to handle requests.
	 */
	public WebSocketService getWebSocketService() {
		return this.webSocketService;
	}


	@Override
	public boolean supports(Object handler) {
		return WebSocketHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebSocketHandler webSocketHandler = (WebSocketHandler) handler;
		return getWebSocketService().handleRequest(exchange, webSocketHandler).then(Mono.empty());
	}

}
