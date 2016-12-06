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

import java.net.URI;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpInbound;
import reactor.ipc.netty.http.HttpOutbound;

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


	protected ReactorNettyWebSocketSession(HttpInbound inbound, HttpOutbound outbound,
			URI uri, NettyDataBufferFactory factory) {

		super(new WebSocketConnection(inbound, outbound), uri, factory);
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		HttpInbound inbound = getDelegate().getHttpInbound();
		return toMessageFlux(inbound.receiveObject().cast(WebSocketFrame.class));
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		HttpOutbound outbound = getDelegate().getHttpOutbound();
		Flux<WebSocketFrame> frameFlux = Flux.from(messages).map(this::toFrame);
		return outbound.sendObject(frameFlux);
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		return Mono.error(new UnsupportedOperationException(
				"Currently in Reactor Netty applications are expected to use the Cancellation" +
						"returned from subscribing to the input Flux to close the WebSocket session."));
	}


	/**
	 * Simple container for {@link HttpInbound} and {@link HttpOutbound}.
	 */
	public static class WebSocketConnection {

		private final HttpInbound inbound;

		private final HttpOutbound outbound;


		public WebSocketConnection(HttpInbound inbound, HttpOutbound outbound) {
			this.inbound = inbound;
			this.outbound = outbound;
		}

		public HttpInbound getHttpInbound() {
			return this.inbound;
		}

		public HttpOutbound getHttpOutbound() {
			return this.outbound;
		}
	}

}
