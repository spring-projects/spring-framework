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

package org.springframework.messaging.tcp.reactor;

import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyOutbound;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpConnection
 * TcpConnection} based on the TCP client support of the Reactor project.
 *
 * @param <P> the payload type of messages read or written to the TCP stream.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class ReactorNettyTcpConnection<P> implements TcpConnection<P> {

	private final NettyInbound                                    in;
	private final NettyOutbound                                   out;
	private final DirectProcessor<Void>                           close;
	private final BiConsumer<? super ByteBuf, ? super Message<P>> encoder;

	public ReactorNettyTcpConnection(NettyInbound in,
			NettyOutbound out,
			BiConsumer<? super ByteBuf, ? super Message<P>> encoder,
			DirectProcessor<Void> close) {
		this.out = out;
		this.in = in;
		this.encoder = encoder;
		this.close = close;
	}

	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		ByteBuf byteBuf = in.channel().alloc().buffer();
		encoder.accept(byteBuf, message);
		return new MonoToListenableFutureAdapter<>(out.send(Mono.just(byteBuf)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		in.onReadIdle(inactivityDuration, runnable);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		out.onWriteIdle(inactivityDuration, runnable);
	}

	@Override
	public void close() {
		close.onComplete();
	}

}
