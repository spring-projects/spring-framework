/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.tcp.reactor;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.group.ChannelGroup;
import io.netty5.channel.group.DefaultChannelGroup;
import io.netty5.handler.codec.ByteToMessageDecoder;
import io.netty5.util.concurrent.ImmediateEventExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty5.Connection;
import reactor.netty5.NettyInbound;
import reactor.netty5.NettyOutbound;
import reactor.netty5.resources.ConnectionProvider;
import reactor.netty5.resources.LoopResources;
import reactor.netty5.tcp.TcpClient;
import reactor.util.retry.Retry;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;

/**
 * Reactor Netty based implementation of {@link TcpOperations}.
 *
 * <p>This class is based on {@link ReactorNettyTcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @param <P> the type of payload for in and outbound messages
 */
public class ReactorNetty2TcpClient<P> implements TcpOperations<P> {

	private static final int PUBLISH_ON_BUFFER_SIZE = 16;


	private final TcpClient tcpClient;

	private final TcpMessageCodec<P> codec;

	@Nullable
	private final ChannelGroup channelGroup;

	@Nullable
	private final LoopResources loopResources;

	@Nullable
	private final ConnectionProvider poolResources;

	private final Scheduler scheduler = Schedulers.newParallel("tcp-client-scheduler");

	private Log logger = LogFactory.getLog(ReactorNetty2TcpClient.class);

	private volatile boolean stopping;


	/**
	 * Simple constructor with the host and port to use to connect to.
	 * <p>This constructor manages the lifecycle of the {@link TcpClient} and
	 * underlying resources such as {@link ConnectionProvider},
	 * {@link LoopResources}, and {@link ChannelGroup}.
	 * <p>For full control over the initialization and lifecycle of the
	 * TcpClient, use {@link #ReactorNetty2TcpClient(TcpClient, TcpMessageCodec)}.
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param codec for encoding and decoding the input/output byte streams
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	public ReactorNetty2TcpClient(String host, int port, TcpMessageCodec<P> codec) {
		Assert.notNull(host, "host is required");
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.loopResources = LoopResources.create("tcp-client-loop");
		this.poolResources = ConnectionProvider.create("tcp-client-pool", 10000);
		this.codec = codec;

		this.tcpClient = TcpClient.create(this.poolResources)
				.host(host).port(port)
				.runOn(this.loopResources, false)
				.doOnConnected(conn -> this.channelGroup.add(conn.channel()));
	}

	/**
	 * A variant of {@link #ReactorNetty2TcpClient(String, int, TcpMessageCodec)}
	 * that still manages the lifecycle of the {@link TcpClient} and underlying
	 * resources, but allows for direct configuration of other properties of the
	 * client through a {@code Function<TcpClient, TcpClient>}.
	 * @param clientConfigurer the configurer function
	 * @param codec for encoding and decoding the input/output byte streams
	 * @since 5.1.3
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	public ReactorNetty2TcpClient(Function<TcpClient, TcpClient> clientConfigurer, TcpMessageCodec<P> codec) {
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.loopResources = LoopResources.create("tcp-client-loop");
		this.poolResources = ConnectionProvider.create("tcp-client-pool", 10000);
		this.codec = codec;

		this.tcpClient = clientConfigurer.apply(TcpClient
				.create(this.poolResources)
				.runOn(this.loopResources, false)
				.doOnConnected(conn -> this.channelGroup.add(conn.channel())));
	}

	/**
	 * Constructor with an externally created {@link TcpClient} instance whose
	 * lifecycle is expected to be managed externally.
	 * @param tcpClient the TcpClient instance to use
	 * @param codec for encoding and decoding the input/output byte streams
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	public ReactorNetty2TcpClient(TcpClient tcpClient, TcpMessageCodec<P> codec) {
		Assert.notNull(tcpClient, "TcpClient is required");
		Assert.notNull(codec, "ReactorNettyCodec is required");
		this.tcpClient = tcpClient;
		this.codec = codec;

		this.channelGroup = null;
		this.loopResources = null;
		this.poolResources = null;
	}


	/**
	 * Set an alternative logger to use than the one based on the class name.
	 * @param logger the logger to use
	 * @since 5.1
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
	}

	/**
	 * Return the currently configured Logger.
	 * @since 5.1
	 */
	public Log getLogger() {
		return logger;
	}


	@Override
	public CompletableFuture<Void> connectAsync(TcpConnectionHandler<P> handler) {
		Assert.notNull(handler, "TcpConnectionHandler is required");

		if (this.stopping) {
			return handleShuttingDownConnectFailure(handler);
		}

		return extendTcpClient(this.tcpClient, handler)
				.handle(new ReactorNettyHandler(handler))
				.connect()
				.doOnError(handler::afterConnectFailure)
				.then().toFuture();
	}

	/**
	 * Provides an opportunity to initialize the {@link TcpClient} for the given
	 * {@link TcpConnectionHandler} which may implement sub-interfaces such as
	 * {@link org.springframework.messaging.simp.stomp.StompTcpConnectionHandler}
	 * that expose further information.
	 * @param tcpClient the candidate TcpClient
	 * @param handler the handler for the TCP connection
	 * @return the same handler or an updated instance
	 */
	protected TcpClient extendTcpClient(TcpClient tcpClient, TcpConnectionHandler<P> handler) {
		return tcpClient;
	}

	@Override
	public CompletableFuture<Void> connectAsync(TcpConnectionHandler<P> handler, ReconnectStrategy strategy) {
		Assert.notNull(handler, "TcpConnectionHandler is required");
		Assert.notNull(strategy, "ReconnectStrategy is required");

		if (this.stopping) {
			return handleShuttingDownConnectFailure(handler);
		}

		// Report first connect to the ListenableFuture
		CompletableFuture<Void> connectFuture = new CompletableFuture<>();

		extendTcpClient(this.tcpClient, handler)
				.handle(new ReactorNettyHandler(handler))
				.connect()
				.doOnNext(conn -> connectFuture.complete(null))
				.doOnError(connectFuture::completeExceptionally)
				.doOnError(handler::afterConnectFailure)    // report all connect failures to the handler
				.flatMap(Connection::onDispose)             // post-connect issues
				.retryWhen(Retry.from(signals -> signals
						.map(retrySignal -> (int) retrySignal.totalRetriesInARow())
						.flatMap(attempt -> reconnect(attempt, strategy))))
				.repeatWhen(flux -> flux
						.scan(1, (count, element) -> count++)
						.flatMap(attempt -> reconnect(attempt, strategy)))
				.subscribe();
		return connectFuture;
	}

	private CompletableFuture<Void> handleShuttingDownConnectFailure(TcpConnectionHandler<P> handler) {
		IllegalStateException ex = new IllegalStateException("Shutting down.");
		handler.afterConnectFailure(ex);
		return Mono.<Void>error(ex).toFuture();
	}

	private Publisher<? extends Long> reconnect(Integer attempt, ReconnectStrategy reconnectStrategy) {
		Long time = reconnectStrategy.getTimeToNextAttempt(attempt);
		return (time != null ? Mono.delay(Duration.ofMillis(time), this.scheduler) : Mono.empty());
	}

	@Override
	public CompletableFuture<Void> shutdownAsync() {
		if (this.stopping) {
			return CompletableFuture.completedFuture(null);
		}

		this.stopping = true;

		Mono<Void> result;
		if (this.channelGroup != null) {
			Sinks.Empty<Void> channnelGroupCloseSink = Sinks.empty();
			this.channelGroup.close().addListener(future -> channnelGroupCloseSink.tryEmitEmpty());
			result = channnelGroupCloseSink.asMono();
			if (this.loopResources != null) {
				result = result.onErrorComplete().then(this.loopResources.disposeLater());
			}
			if (this.poolResources != null) {
				result = result.onErrorComplete().then(this.poolResources.disposeLater());
			}
			result = result.onErrorComplete().then(stopScheduler());
		}
		else {
			result = stopScheduler();
		}

		return result.toFuture();
	}

	private Mono<Void> stopScheduler() {
		return Mono.fromRunnable(() -> {
			this.scheduler.dispose();
			for (int i = 0; i < 20; i++) {
				if (this.scheduler.isDisposed()) {
					break;
				}
				try {
					Thread.sleep(100);
				}
				catch (Throwable ex) {
					break;
				}
			}
		});
	}

	@Override
	public String toString() {
		return "ReactorNetty2TcpClient[" + this.tcpClient + "]";
	}


	private class ReactorNettyHandler implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

		private final TcpConnectionHandler<P> connectionHandler;

		ReactorNettyHandler(TcpConnectionHandler<P> handler) {
			this.connectionHandler = handler;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Publisher<Void> apply(NettyInbound inbound, NettyOutbound outbound) {
			inbound.withConnection(conn -> {
				if (logger.isDebugEnabled()) {
					logger.debug("Connected to " + conn.address());
				}
			});
			Sinks.Empty<Void> completionSink = Sinks.empty();
			TcpConnection<P> connection = new ReactorNetty2TcpConnection<>(inbound, outbound, codec, completionSink);
			scheduler.schedule(() -> this.connectionHandler.afterConnected(connection));

			inbound.withConnection(conn -> conn.addHandlerFirst(new StompMessageDecoder<>(codec)));

			inbound.receiveObject()
					.cast(Message.class)
					.publishOn(scheduler, PUBLISH_ON_BUFFER_SIZE)
					.subscribe(
							this.connectionHandler::handleMessage,
							this.connectionHandler::handleFailure,
							this.connectionHandler::afterConnectionClosed);

			return completionSink.asMono();
		}
	}


	private static class StompMessageDecoder<P> extends ByteToMessageDecoder {

		private final TcpMessageCodec<P> codec;

		StompMessageDecoder(TcpMessageCodec<P> codec) {
			this.codec = codec;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, Buffer buffer) throws Exception {
			ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.readableBytes());
			buffer.readBytes(byteBuffer);
			byteBuffer.flip();
			List<Message<P>> messages = this.codec.decode(byteBuffer);
			for (Message<P> message : messages) {
				ctx.fireChannelRead(message);
			}
		}

	}

}
