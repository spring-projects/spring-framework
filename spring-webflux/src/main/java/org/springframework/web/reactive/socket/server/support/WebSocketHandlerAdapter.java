/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link HandlerAdapter} that allows using a {@link WebSocketHandler} with the
 * generic {@link DispatcherHandler} mapping URLs directly to such handlers.
 * Requests are handled by delegating to the configured {@link WebSocketService}
 * which by default is {@link HandshakeWebSocketService}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketHandlerAdapter implements HandlerAdapter {

	private final WebSocketService webSocketService;


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
