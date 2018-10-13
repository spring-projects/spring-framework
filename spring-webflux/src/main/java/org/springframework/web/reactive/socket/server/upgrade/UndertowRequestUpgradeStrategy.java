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

package org.springframework.web.reactive.socket.server.upgrade;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
* A {@link RequestUpgradeStrategy} for use with Undertow.
  *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowRequestUpgradeStrategy implements RequestUpgradeStrategy {

	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpRequest request = exchange.getRequest();
		Assert.isInstanceOf(AbstractServerHttpRequest.class, request);
		HttpServerExchange httpExchange = ((AbstractServerHttpRequest) request).getNativeRequest();

		Set<String> protocols = (subProtocol != null ? Collections.singleton(subProtocol) : Collections.emptySet());
		Hybi13Handshake handshake = new Hybi13Handshake(protocols, false);
		List<Handshake> handshakes = Collections.singletonList(handshake);

		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

		try {
			DefaultCallback callback = new DefaultCallback(handshakeInfo, handler, bufferFactory);
			new WebSocketProtocolHandshakeHandler(handshakes, callback).handleRequest(httpExchange);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}

		return Mono.empty();
	}


	private class DefaultCallback implements WebSocketConnectionCallback {

		private final HandshakeInfo handshakeInfo;

		private final WebSocketHandler handler;

		private final DataBufferFactory bufferFactory;

		public DefaultCallback(HandshakeInfo handshakeInfo, WebSocketHandler handler, DataBufferFactory bufferFactory) {
			this.handshakeInfo = handshakeInfo;
			this.handler = handler;
			this.bufferFactory = bufferFactory;
		}

		@Override
		public void onConnect(WebSocketHttpExchange httpExchange, WebSocketChannel channel) {
			UndertowWebSocketSession session = createSession(channel);
			UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

			channel.getReceiveSetter().set(adapter);
			channel.resumeReceives();

			this.handler.handle(session).subscribe(session);
		}

		private UndertowWebSocketSession createSession(WebSocketChannel channel) {
			return new UndertowWebSocketSession(channel, this.handshakeInfo, this.bufferFactory);
		}
	}

}
