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

package org.springframework.web.reactive.socket.adapter;

import java.util.function.Consumer;

import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * {@link WebSocketSession} implementation for use with the Reactor Netty's
 * {@link NettyInbound} and {@link NettyOutbound}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketSession
		extends NettyWebSocketSessionSupport<ReactorNettyWebSocketSession.WebSocketConnection> {

	private final int maxFramePayloadLength;

	private final ChannelId channelId;


	/**
	 * Constructor for the session, using the {@link #DEFAULT_FRAME_MAX_SIZE} value.
	 */
	public ReactorNettyWebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound,
			HandshakeInfo info, NettyDataBufferFactory bufferFactory) {

		this(inbound, outbound, info, bufferFactory, DEFAULT_FRAME_MAX_SIZE);
	}

	/**
	 * Constructor with an additional maxFramePayloadLength argument.
	 * @since 5.1
	 */
	public ReactorNettyWebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound,
			HandshakeInfo info, NettyDataBufferFactory bufferFactory,
			int maxFramePayloadLength) {

		super(new WebSocketConnection(inbound, outbound), info, bufferFactory);
		this.maxFramePayloadLength = maxFramePayloadLength;
		this.channelId = ((ChannelOperations) inbound).channel().id();
	}


	/**
	 * Return the id of the underlying Netty channel.
	 * @since 5.3.4
	 */
	public ChannelId getChannelId() {
		return this.channelId;
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		return getDelegate().getInbound()
				.aggregateFrames(this.maxFramePayloadLength)
				.receiveFrames()
				.map(super::toMessage)
				.doOnNext(message -> {
					if (logger.isTraceEnabled()) {
						logger.trace(getLogPrefix() + "Received " + message);
					}
				});
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		Flux<WebSocketFrame> frames = Flux.from(messages)
				.doOnNext(message -> {
					if (logger.isTraceEnabled()) {
						logger.trace(getLogPrefix() + "Sending " + message);
					}
				})
				.map(this::toFrame);
		return getDelegate().getOutbound()
				.sendObject(frames)
				.then();
	}

	@Override
	public boolean isOpen() {
		DisposedCallback callback = new DisposedCallback();
		getDelegate().getInbound().withConnection(callback);
		return !callback.isDisposed();
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		// this will notify WebSocketInbound.receiveCloseStatus()
		return getDelegate().getOutbound().sendClose(status.getCode(), status.getReason());
	}

	@Override
	public Mono<CloseStatus> closeStatus() {
		return getDelegate().getInbound().receiveCloseStatus()
				.map(status -> CloseStatus.create(status.code(), status.reasonText()));
	}

	/**
	 * Simple container for {@link NettyInbound} and {@link NettyOutbound}.
	 */
	public static class WebSocketConnection {

		private final WebsocketInbound inbound;

		private final WebsocketOutbound outbound;


		public WebSocketConnection(WebsocketInbound inbound, WebsocketOutbound outbound) {
			this.inbound = inbound;
			this.outbound = outbound;
		}

		public WebsocketInbound getInbound() {
			return this.inbound;
		}

		public WebsocketOutbound getOutbound() {
			return this.outbound;
		}
	}


	private static class DisposedCallback implements Consumer<Connection> {

		private boolean disposed;

		public boolean isDisposed() {
			return this.disposed;
		}

		@Override
		public void accept(Connection connection) {
			this.disposed = connection.isDisposed();
		}
	}

}
