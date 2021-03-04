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

import java.util.function.BiPredicate;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;

/**
 * A predicate for use with
 * {@link org.springframework.web.reactive.handler.AbstractUrlHandlerMapping#setHandlerPredicate}
 * to ensure only WebSocket handshake requests are matched to handlers of type
 * {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.5
 */
public class WebSocketUpgradeHandlerPredicate implements BiPredicate<Object, ServerWebExchange> {

	@Override
	public boolean test(Object handler, ServerWebExchange exchange) {
		if (handler instanceof WebSocketHandler) {
			String method = exchange.getRequest().getMethodValue();
			String header = exchange.getRequest().getHeaders().getUpgrade();
			return (method.equals("GET") && header != null && header.equalsIgnoreCase("websocket"));
		}
		return true;
	}

}
