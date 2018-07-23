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

package org.springframework.messaging.tcp.reactor;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MonoToListenableFutureAdapter;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Reactor Netty based implementation of {@link TcpConnection}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <P> the type of payload for outbound messages
 */
public class ReactorNettyTcpConnection<P> implements TcpConnection<P> {

	private final NettyInbound inbound;

	private final NettyOutbound outbound;

	private final ReactorNettyCodec<P> codec;

	private final DirectProcessor<Void> closeProcessor;


	public ReactorNettyTcpConnection(NettyInbound inbound, NettyOutbound outbound,
			ReactorNettyCodec<P> codec, DirectProcessor<Void> closeProcessor) {

		this.inbound = inbound;
		this.outbound = outbound;
		this.codec = codec;
		this.closeProcessor = closeProcessor;
	}


	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		ByteBuf byteBuf = this.outbound.alloc().buffer();
		this.codec.encode(message, byteBuf);
		Mono<Void> sendCompletion = this.outbound.send(Mono.just(byteBuf)).then();
		return new MonoToListenableFutureAdapter<>(sendCompletion);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.inbound.withConnection(conn -> conn.onReadIdle(inactivityDuration, runnable));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.inbound.withConnection(conn -> conn.onWriteIdle(inactivityDuration, runnable));
	}

	@Override
	public void close() {
		this.closeProcessor.onComplete();
	}

}
