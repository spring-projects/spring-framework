/*
 * Copyright 2002-2017 the original author or authors.
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
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.FutureMono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.options.ClientOptions;
import reactor.ipc.netty.resources.LoopResources;
import reactor.ipc.netty.resources.PoolResources;
import reactor.ipc.netty.tcp.TcpClient;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Reactor Netty based implementation of {@link TcpOperations}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 */
public class ReactorNettyTcpClient<P> implements TcpOperations<P> {

	private static final int PUBLISH_ON_BUFFER_SIZE = 16;


	private final TcpClient tcpClient;

	private final ReactorNettyCodec<P> codec;

	@Nullable
	private final ChannelGroup channelGroup;

	@Nullable
	private LoopResources loopResources;

	@Nullable
	private PoolResources poolResources;

	private final Scheduler scheduler = Schedulers.newParallel("tcp-client-scheduler");

	private volatile boolean stopping = false;


	/**
	 * Simple constructor with a host and a port.
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param codec the code to use
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	public ReactorNettyTcpClient(String host, int port, ReactorNettyCodec<P> codec) {
		this(builder -> builder.host(host).port(port), codec);
	}

	/**
	 * Constructor with a {@link ClientOptions.Builder} that can be used to
	 * customize Reactor Netty client options.
	 *
	 * <p><strong>Note: </strong> this constructor manages the lifecycle of the
	 * {@link TcpClient} and its underlying resources. Please do not customize
	 * any of the following options:
	 * {@link ClientOptions.Builder#channelGroup(ChannelGroup) ChannelGroup},
	 * {@link ClientOptions.Builder#loopResources(LoopResources) LoopResources}, and
	 * {@link ClientOptions.Builder#poolResources(PoolResources) PoolResources}.
	 * You may set the {@link ClientOptions.Builder#disablePool() disablePool}
	 * option if you simply want to turn off pooling.
	 *
	 * <p>For full control over the initialization and lifecycle of the TcpClient,
	 * see {@link #ReactorNettyTcpClient(TcpClient, ReactorNettyCodec)}.
	 *
	 * @param optionsConsumer consumer to customize client options
	 * @param codec the code to use
	 * @see org.springframework.messaging.simp.stomp.StompReactorNettyCodec
	 */
	public ReactorNettyTcpClient(Consumer<ClientOptions.Builder<?>> optionsConsumer,
			ReactorNettyCodec<P> codec) {

		Assert.notNull(optionsConsumer, "Consumer<ClientOptions.Builder<?> is required");
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

		Consumer<ClientOptions.Builder<?>> builtInConsumer = builder -> {

			Assert.isTrue(!builder.isLoopAvailable() && !builder.isPoolAvailable(),
					"The provided ClientOptions.Builder contains LoopResources and/or PoolResources. " +
							"Please, use the constructor that accepts a TcpClient instance " +
							"for full control over initialization and lifecycle.");

			builder.channelGroup(this.channelGroup);
			builder.preferNative(false);

			this.loopResources = LoopResources.create("tcp-client-loop");
			builder.loopResources(this.loopResources);

			if (!builder.isPoolDisabled()) {
				this.poolResources = PoolResources.elastic("tcp-client-pool");
				builder.poolResources(this.poolResources);
			}
		};

		this.tcpClient = TcpClient.create(optionsConsumer.andThen(builtInConsumer));
		this.codec = codec;
	}

	/**
	 * Constructor with an externally created {@link TcpClient} instance whose
	 * lifecycle is expected to be managed externally.
	 *
	 * @param tcpClient the TcpClient instance to use
	 * @param codec the code to use
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


	@Override
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> handler) {
		Assert.notNull(handler, "TcpConnectionHandler is required");

		if (this.stopping) {
			return handleShuttingDownConnectFailure(handler);
		}

		Mono<Void> connectMono = this.tcpClient
				.newHandler(new ReactorNettyHandler(handler))
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
				.newHandler(new ReactorNettyHandler(handler))
				.doOnNext(updateConnectMono(connectMono))
				.doOnError(updateConnectMono(connectMono))
				.doOnError(handler::afterConnectFailure)    // report all connect failures to the handler
				.flatMap(NettyContext::onClose)                // post-connect issues
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


	private class ReactorNettyHandler implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

		private final TcpConnectionHandler<P> connectionHandler;

		ReactorNettyHandler(TcpConnectionHandler<P> handler) {
			this.connectionHandler = handler;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Publisher<Void> apply(NettyInbound inbound, NettyOutbound outbound) {
			DirectProcessor<Void> completion = DirectProcessor.create();
			TcpConnection<P> connection = new ReactorNettyTcpConnection<>(inbound, outbound,  codec, completion);
			scheduler.schedule(() -> connectionHandler.afterConnected(connection));

			inbound.context().addHandler(new StompMessageDecoder<>(codec));

			inbound.receiveObject()
					.cast(Message.class)
					.publishOn(scheduler, PUBLISH_ON_BUFFER_SIZE)
					.subscribe(
							connectionHandler::handleMessage,
							connectionHandler::handleFailure,
							connectionHandler::afterConnectionClosed);

			return completion;
		}
	}


	private static class StompMessageDecoder<P> extends ByteToMessageDecoder {

		private final ReactorNettyCodec<P> codec;

		public StompMessageDecoder(ReactorNettyCodec<P> codec) {
			this.codec = codec;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			Collection<Message<P>> messages = codec.decode(in);
			out.addAll(messages);
		}
	}

}
