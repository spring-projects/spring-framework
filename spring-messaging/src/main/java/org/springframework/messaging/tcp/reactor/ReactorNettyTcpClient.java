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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
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
import reactor.ipc.netty.tcp.TcpResources;
import reactor.util.concurrent.QueueSupplier;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
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

	private final TcpClient tcpClient;

	private final ReactorNettyCodec<P> codec;

	private final ChannelGroup channelGroup;

	private final LoopResources loopResources;

	private final PoolResources poolResources;

	private final Scheduler scheduler = Schedulers.newParallel("ReactorNettyTcpClient");

	private volatile boolean stopping = false;


	/**
	 * Basic constructor with a host and a port.
	 */
	public ReactorNettyTcpClient(String host, int port, ReactorNettyCodec<P> codec) {
		this(opts -> opts.connect(host, port), codec);
	}

	/**
	 * Alternate constructor with a {@link ClientOptions} consumer providing
	 * additional control beyond a host and a port.
	 */
	public ReactorNettyTcpClient(Consumer<ClientOptions> optionsConsumer, ReactorNettyCodec<P> codec) {
		Assert.notNull(optionsConsumer, "Consumer<ClientOptions> is required");
		Assert.notNull(codec, "ReactorNettyCodec is required");

		this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.loopResources = LoopResources.create("reactor-netty-tcp-client");
		this.poolResources = PoolResources.fixed("reactor-netty-tcp-pool");

		Consumer<ClientOptions> builtInConsumer = opts -> opts
				.channelGroup(this.channelGroup)
				.loopResources(this.loopResources)
				.poolResources(this.poolResources)
				.preferNative(false);

		this.tcpClient = TcpClient.create(optionsConsumer.andThen(builtInConsumer));
		this.codec = codec;
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
				.then(NettyContext::onClose)                // post-connect issues
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
				.flatMap(attempt -> Mono.delayMillis(reconnectStrategy.getTimeToNextAttempt(attempt)));
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		if (this.stopping) {
			SettableListenableFuture<Void> future = new SettableListenableFuture<>();
			future.set(null);
			return future;
		}

		this.stopping = true;

		ChannelGroupFuture close = this.channelGroup.close();
		Mono<Void> completion = FutureMono.from(close)
				.doAfterTerminate((x, e) -> {

					// TODO: https://github.com/reactor/reactor-netty/issues/24
					shutdownGlobalResources();

					this.loopResources.dispose();
					this.poolResources.dispose();

					// TODO: https://github.com/reactor/reactor-netty/issues/25
					try {
						Thread.sleep(2000);
					}
					catch (InterruptedException ex) {
						ex.printStackTrace();
					}

					// Scheduler after loop resources...
					this.scheduler.dispose();
				});

		return new MonoToListenableFutureAdapter<>(completion);
	}

	private static void shutdownGlobalResources() {
		try {
			Method method = TcpResources.class.getDeclaredMethod("_dispose");
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, TcpResources.get());
		}
		catch (NoSuchMethodException ex) {
			ex.printStackTrace();
		}
	}


	private class ReactorNettyHandler implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

		private final TcpConnectionHandler<P> connectionHandler;


		ReactorNettyHandler(TcpConnectionHandler<P> handler) {
			this.connectionHandler = handler;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Publisher<Void> apply(NettyInbound inbound, NettyOutbound outbound) {

			DirectProcessor<Void> completion = DirectProcessor.create();
			TcpConnection<P> connection = new ReactorNettyTcpConnection<>(inbound, outbound,  codec, completion);
			scheduler.schedule(() -> connectionHandler.afterConnected(connection));

			inbound.context().addHandler(new StompMessageDecoder<>(codec));

			inbound.receiveObject()
					.cast(Message.class)
					.publishOn(scheduler, QueueSupplier.SMALL_BUFFER_SIZE)
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
