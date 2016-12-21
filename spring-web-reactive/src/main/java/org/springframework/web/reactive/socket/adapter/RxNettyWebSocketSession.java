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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} implementation that adapts to the RxNetty
 * {@link io.reactivex.netty.protocol.http.ws.WebSocketConnection}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyWebSocketSession extends NettyWebSocketSessionSupport<WebSocketConnection> {

	/**
	 * The name of the {@link WebSocketFrameAggregator} inserted by
	 * {@link #aggregateFrames(Channel, String)}.
	 */
	public static final String FRAME_AGGREGATOR_NAME = "websocket-frame-aggregator";


	public RxNettyWebSocketSession(WebSocketConnection conn, HandshakeInfo info,
			NettyDataBufferFactory factory) {

		super(conn, info, factory);
	}


	/**
	 * Inserts an {@link WebSocketFrameAggregator} after the
	 * {@code WebSocketFrameDecoder} for receiving full messages.
	 * @param channel the channel for the session
	 * @param frameDecoderName the name of the WebSocketFrame decoder
	 */
	public RxNettyWebSocketSession aggregateFrames(Channel channel, String frameDecoderName) {
		ChannelPipeline pipeline = channel.pipeline();
		if (pipeline.context(FRAME_AGGREGATOR_NAME) != null) {
			logger.trace("WebSocketFrameAggregator already registered.");
			return this;
		}
		ChannelHandlerContext context = pipeline.context(frameDecoderName);
		Assert.notNull(context, "WebSocketFrameDecoder not found: " + frameDecoderName);
		WebSocketFrameAggregator aggregator = new WebSocketFrameAggregator(DEFAULT_FRAME_MAX_SIZE);
		pipeline.addAfter(context.name(), FRAME_AGGREGATOR_NAME, aggregator);
		return this;
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		Observable<WebSocketMessage> observable = getDelegate().getInput().map(super::toMessage);
		return Flux.from(RxReactiveStreams.toPublisher(observable));
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		Observable<WebSocketFrame> frames = RxReactiveStreams.toObservable(messages).map(this::toFrame);
		Observable<Void> completion = getDelegate().writeAndFlushOnEach(frames);
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		Observable<Void> completion = getDelegate().close();
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

}
