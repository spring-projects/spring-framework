/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.socket.server;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A strategy for upgrading an HTTP request to a WebSocket interaction depending
 * on the underlying HTTP runtime.
 *
 * <p>Typically there is one such strategy for every {@link ServerHttpRequest}
 * and {@link ServerHttpResponse} implementation type except in the case of
 * Servlet containers for which there is no standard API to upgrade a request.
 * JSR-356 does have programmatic endpoint registration but that is only
 * intended for use on startup and not per request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestUpgradeStrategy {

	/**
	 * Upgrade the request to a WebSocket interaction and adapt the given
	 * Spring {@link WebSocketHandler} to the underlying runtime WebSocket API.
	 * @param exchange the current exchange
	 * @param webSocketHandler handler for WebSocket session
	 * @return a completion Mono for the WebSocket session handling
	 */
	Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler);

}
