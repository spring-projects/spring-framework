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
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

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
public class RxNettyWebSocketSession extends NettyWebSocketSessionSupport<WebSocketConnection> {

	public RxNettyWebSocketSession(WebSocketConnection conn, URI uri, NettyDataBufferFactory factory) {
		super(conn, uri, factory);
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		Observable<WebSocketFrame> observable = getDelegate().getInput();
		Flux<WebSocketFrame> flux = Flux.from(RxReactiveStreams.toPublisher(observable));
		return toMessageFlux(flux);
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		Observable<WebSocketFrame> frames = RxReactiveStreams.toObservable(messages).map(this::toFrame);
		Observable<Void> completion = getDelegate().writeAndFlushOnEach(frames);
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		Observable<Void> completion = getDelegate().close();
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

}
