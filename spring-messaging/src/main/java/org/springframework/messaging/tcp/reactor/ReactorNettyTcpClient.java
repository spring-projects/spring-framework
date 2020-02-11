/*
 * Copyright 2002-2020 the original author or authors.
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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.FutureMono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.MonoToListenableFutureAdapter;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Reactor Netty based implementation of {@link TcpOperations}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 * @param <P> the type of payload for in and outbound messages
 */
public class ReactorNettyTcpClient<P> implements TcpOperations<P> {

	private static final int PUBLISH_ON_BUFFER_SIZE = 16;


	private final TcpClient tcpClient;

	private final ReactorNettyCodec<P> codec;

	@Nullable
	private final ChannelGroup channelGroup;

	@Nullable
	private final LoopResources loopResources;

	@Nullable
	private final ConnectionProvider poolResources;

	private final Scheduler scheduler = Schedulers.newParallel("tcp-client-scheduler");

	private Log logger = LogFactory.getLog(ReactorNettyTcpClient.class);

	private volatile boolean stopping = false;


	/**
	 * Simple constructor with the host and port to use to connect to.
	 * <p>This constructor manages the lifecycle of the {@link TcpClient} and
	 * underlying resources such as {@link ConnectionProvider},
	 * {@link LoopResources}, and {@link ChannelGroup}.
	 * <p>For full control over the initialization and lifecycle of the
	 * TcpClient, use {@link #ReactorNettyTcpClient(TcpClient, ReactorNettyCodec)}.
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param codec for encoding and decoding the input/output byte streams
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	@SuppressWarnings("deprecation")
	public ReactorNettyTcpClient(String host, int port, ReactorNettyCodec<P> codec) {
		Assert.notNull(host, "host is required");
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.loopResources = LoopResources.create("tcp-client-loop");
		this.poolResources = ConnectionProvider.fixed("tcp-client-pool", 10000);
		this.codec = codec;

		this.tcpClient = TcpClient.create(this.poolResources)
				.host(host).port(port)
				.runOn(this.loopResources, false)
				.doOnConnected(conn -> this.channelGroup.add(conn.channel()));
	}

	/**
	 * A variant of {@link #ReactorNettyTcpClient(String, int, ReactorNettyCodec)}
	 * that still manages the lifecycle of the {@link TcpClient} and underlying
	 * resources, but allows for direct configuration of other properties of the
	 * client through a {@code Function<TcpClient, TcpClient>}.
	 * @param clientConfigurer the configurer function
	 * @param codec for encoding and decoding the input/output byte streams
	 * @since 5.1.3
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	@SuppressWarnings("deprecation")
	public ReactorNettyTcpClient(Function<TcpClient, TcpClient> clientConfigurer, ReactorNettyCodec<P> codec) {
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.loopResources = LoopResources.create("tcp-client-loop");
		this.poolResources = ConnectionProvider.fixed("tcp-client-pool", 10000);
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
	public ReactorNettyTcpClient(TcpClient tcpClient, ReactorNettyCodec<P> codec) {
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
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> handler) {
		Assert.notNull(handler, "TcpConnectionHandler is required");

		if (this.stopping) {
			return handleShuttingDownConnectFailure(handler);
		}

		Mono<Void> connectMono = this.tcpClient
				.handle(new ReactorNettyHandler(handler))
				.connect()
				.doOnError(handler::afterConnectFailure)
				.then();

		return new MonoToListenableFutureAdapter<>(connectMono);
	}

	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> handler, ReconnectStrategy strategy) {
		Assert.notNull(handler, "TcpConnectionHandler is required");
		Assert.notNull(strategy, "ReconnectStrategy is required");

		if (this.stopping) {
			return handleShuttingDownConnectFailure(handler);
		}

		// Report first connect to the ListenableFuture
		MonoProcessor<Void> connectMono = MonoProcessor.create();

		this.tcpClient
				.handle(new ReactorNettyHandler(handler))
				.connect()
				.doOnNext(updateConnectMono(connectMono))
				.doOnError(updateConnectMono(connectMono))
				.doOnError(handler::afterConnectFailure)    // report all connect failures to the handler
				.flatMap(Connection::onDispose)             // post-connect issues
				.retryWhen(reconnectFunction(strategy))
				.repeatWhen(reconnectFunction(strategy))
				.subscribe();

		return new MonoToListenableFutureAdapter<>(connectMono);
	}

	private ListenableFuture<Void> handleShuttingDownConnectFailure(TcpConnectionHandler<P> handler) {
		IllegalStateException ex = new IllegalStateException("Shutting down.");
		handler.afterConnectFailure(ex);
		return new MonoToListenableFutureAdapter<>(Mono.error(ex));
	}

	private <T> Consumer<T> updateConnectMono(MonoProcessor<Void> connectMono) {
		return o -> {
			if (!connectMono.isTerminated()) {
				if (o instanceof Throwable) {
					connectMono.onError((Throwable) o);
				}
				else {
					connectMono.onComplete();
				}
			}
		};
	}

	private <T> Function<Flux<T>, Publisher<?>> reconnectFunction(ReconnectStrategy reconnectStrategy) {
		return flux -> flux
				.scan(1, (count, element) -> count++)
				.flatMap(attempt -> Optional.ofNullable(reconnectStrategy.getTimeToNextAttempt(attempt))
						.map(time -> Mono.delay(Duration.ofMillis(time), this.scheduler))
						.orElse(Mono.empty()));
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		if (this.stopping) {
			SettableListenableFuture<Void> future = new SettableListenableFuture<>();
			future.set(null);
			return future;
		}

		this.stopping = true;

		Mono<Void> result;
		if (this.channelGroup != null) {
			result = FutureMono.from(this.channelGroup.close());
			if (this.loopResources != null) {
				result = result.onErrorResume(ex -> Mono.empty()).then(this.loopResources.disposeLater());
			}
			if (this.poolResources != null) {
				result = result.onErrorResume(ex -> Mono.empty()).then(this.poolResources.disposeLater());
			}
			result = result.onErrorResume(ex -> Mono.empty()).then(stopScheduler());
		}
		else {
			result = stopScheduler();
		}

		return new MonoToListenableFutureAdapter<>(result);
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
		return "ReactorNettyTcpClient[" + this.tcpClient + "]";
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
			DirectProcessor<Void> completion = DirectProcessor.create();
			TcpConnection<P> connection = new ReactorNettyTcpConnection<>(inbound, outbound,  codec, completion);
			scheduler.schedule(() -> this.connectionHandler.afterConnected(connection));

			inbound.withConnection(conn -> conn.addHandler(new StompMessageDecoder<>(codec)));

			inbound.receiveObject()
					.cast(Message.class)
					.publishOn(scheduler, PUBLISH_ON_BUFFER_SIZE)
					.subscribe(
							this.connectionHandler::handleMessage,
							this.connectionHandler::handleFailure,
							this.connectionHandler::afterConnectionClosed);

			return completion;
		}
	}


	private static class StompMessageDecoder<P> extends ByteToMessageDecoder {

		private final ReactorNettyCodec<P> codec;

		public StompMessageDecoder(ReactorNettyCodec<P> codec) {
			this.codec = codec;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
			Collection<Message<P>> messages = this.codec.decode(in);
			out.addAll(messages);
		}
	}

}
