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
package org.springframework.web.reactive.socket.adapter;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.http.websocket.WebsocketInbound;
import reactor.ipc.netty.http.websocket.WebsocketOutbound;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;


/**
 * Spring {@link WebSocketSession} adapter for RxNetty's
 * {@link io.reactivex.netty.protocol.http.ws.WebSocketConnection}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketSession
		extends NettyWebSocketSessionSupport<ReactorNettyWebSocketSession.WebSocketConnection> {


	public ReactorNettyWebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound,
			HandshakeInfo handshakeInfo, NettyDataBufferFactory bufferFactory) {

		super(new WebSocketConnection(inbound, outbound), handshakeInfo, bufferFactory);
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		WebsocketInbound inbound = getDelegate().getWebsocketInbound();
		return toMessageFlux(inbound.receiveObject().cast(WebSocketFrame.class));
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		Flux<WebSocketFrame> frameFlux = Flux.from(messages).map(this::toFrame);
		WebsocketOutbound outbound = getDelegate().getWebsocketOutbound();
		return outbound.options(NettyPipeline.SendOptions::flushOnEach)
		               .sendObject(frameFlux)
		               .then();
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		return Mono.error(new UnsupportedOperationException(
				"Currently in Reactor Netty applications are expected to use the Cancellation" +
						"returned from subscribing to the input Flux to close the WebSocket session."));
	}


	/**
	 * Simple container for {@link WebsocketInbound} and {@link WebsocketOutbound}.
	 */
	public static class WebSocketConnection {

		private final WebsocketInbound inbound;

		private final WebsocketOutbound outbound;

		public WebSocketConnection(WebsocketInbound inbound, WebsocketOutbound outbound) {
			this.inbound = inbound;
			this.outbound = outbound;
		}

		public WebsocketInbound getWebsocketInbound() {
			return this.inbound;
		}

		public WebsocketOutbound getWebsocketOutbound() {
			return this.outbound;
		}
	}

}
