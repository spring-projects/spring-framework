/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp.reactor;

import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;

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

	private final Sinks.Empty<Void> completionSink;


	public ReactorNettyTcpConnection(NettyInbound inbound, NettyOutbound outbound,
			ReactorNettyCodec<P> codec, Sinks.Empty<Void> completionSink) {

		this.inbound = inbound;
		this.outbound = outbound;
		this.codec = codec;
		this.completionSink = completionSink;
	}


	@Override
	public CompletableFuture<@Nullable Void> sendAsync(Message<P> message) {
		ByteBuf byteBuf = this.outbound.alloc().buffer();
		this.codec.encode(message, byteBuf);
		return this.outbound.send(Mono.just(byteBuf))
				.then()
				.toFuture();
	}

	@Override
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.inbound.withConnection(conn -> conn.onReadIdle(inactivityDuration, runnable));
	}

	@Override
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.inbound.withConnection(conn -> conn.onWriteIdle(inactivityDuration, runnable));
	}

	@Override
	public void close() {
		// Ignore result: concurrent attempts to complete are ok
		this.completionSink.tryEmitEmpty();
	}

	@Override
	public String toString() {
		return "ReactorNettyTcpConnection[inbound=" + this.inbound + "]";
	}

}
