/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.support;

import java.util.function.BiPredicate;

import org.springframework.http.HttpMethod;
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
			HttpMethod method = exchange.getRequest().getMethod();
			String header = exchange.getRequest().getHeaders().getUpgrade();
			return (HttpMethod.GET.equals(method) && header != null && header.equalsIgnoreCase("websocket"));
		}
		return true;
	}

}
