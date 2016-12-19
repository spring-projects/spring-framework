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

package org.springframework.web.reactive.socket.server.upgrade;

import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.UndertowServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
* A {@link RequestUpgradeStrategy} for use with Undertow.
  * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class UndertowRequestUpgradeStrategy implements RequestUpgradeStrategy {


	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		HandshakeInfo info = getHandshakeInfo(exchange);
		DataBufferFactory bufferFactory = response.bufferFactory();

		Assert.isTrue(request instanceof UndertowServerHttpRequest);
		HttpServerExchange httpExchange = ((UndertowServerHttpRequest) request).getUndertowExchange();

		WebSocketConnectionCallback callback =
				new UndertowWebSocketHandlerAdapter(handler, info, bufferFactory);

		try {
			new WebSocketProtocolHandshakeHandler(callback).handleRequest(httpExchange);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}

		return Mono.empty();
	}

	private HandshakeInfo getHandshakeInfo(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		return new HandshakeInfo(request.getURI(), request.getHeaders(), exchange.getPrincipal());
	}

}
