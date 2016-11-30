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

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.ipc.netty.ChannelFutureMono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.options.ClientOptions;
import reactor.ipc.netty.tcp.TcpClient;
import reactor.util.concurrent.QueueSupplier;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpOperations}
 * based on the TCP client support of the Reactor project.
 * <p>
 * <p>This implementation wraps N (Reactor) clients for N {@link #connect} calls,
 * i.e. a separate (Reactor) client instance for each connection.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 4.2
 */
public class ReactorNettyTcpClient<P> implements TcpOperations<P> {

	private final TcpClient                      tcpClient;

	private final MessageHandlerConfiguration<P> configuration;
	private final ChannelGroup                   group;
	private volatile boolean stopping;

	/**
	 * A constructor that creates a {@link TcpClient TcpClient} factory relying on
	 * Reactor Netty TCP threads. The number of Netty threads can be tweaked with
	 * the {@code reactor.tcp.ioThreadCount} System property. The network I/O
	 * threads will be shared amongst the active clients.
	 * <p>Also see the constructor accepting a {@link Consumer} of
	 * {@link ClientOptions} for advanced tuning.
	 *
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param configuration the client configuration
	 */
	public ReactorNettyTcpClient(String host,
			int port,
			MessageHandlerConfiguration<P> configuration) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.tcpClient = TcpClient.create(options -> options.connect(host, port)
		                                                    .channelGroup(group));
	}

	/**
	 * A constructor with a configurator {@link Consumer} that will receive default {@link
	 * ClientOptions} from {@link TcpClient}. This might be used to add SSL or specific
	 * network parameters to the generated client configuration.
	 *
	 * @param tcpOptions the {@link Consumer} of {@link ClientOptions} shared to use by
	 * connected handlers.
	 * @param configuration the client configuration
	 */
	public ReactorNettyTcpClient(Consumer<? super ClientOptions> tcpOptions,
			MessageHandlerConfiguration<P> configuration) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		this.tcpClient =
				TcpClient.create(opts -> tcpOptions.accept(opts.channelGroup(group)));
	}

	@Override
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> connectionHandler) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
		if (stopping) {
			IllegalStateException ex = new IllegalStateException("Shutting down.");
			connectionHandler.afterConnectFailure(ex);
			return new MonoToListenableFutureAdapter<>(Mono.<Void>error(ex));
		}

		MessageHandler<P> handler =
				new MessageHandler<>(connectionHandler, configuration);

		Mono<Void> promise = tcpClient.newHandler(handler)
		                              .doOnError(connectionHandler::afterConnectFailure)
		                              .then();

		return new MonoToListenableFutureAdapter<>(promise);
	}

	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler,
			ReconnectStrategy strategy) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
		Assert.notNull(strategy, "ReconnectStrategy must not be null");

		if (stopping) {
			IllegalStateException ex = new IllegalStateException("Shutting down.");
			connectionHandler.afterConnectFailure(ex);
			return new MonoToListenableFutureAdapter<>(Mono.<Void>error(ex));
		}

		MessageHandler<P> handler =
				new MessageHandler<>(connectionHandler, configuration);

		MonoProcessor<Void> promise = MonoProcessor.create();

		tcpClient.newHandler(handler)
		         .doOnNext(e -> {
			         if (!promise.isTerminated()) {
				         promise.onComplete();
			         }
		         })
		         .doOnError(e -> {
			         if (!promise.isTerminated()) {
				         promise.onError(e);
			         }
		         })
		         .then(NettyContext::onClose)
		         .retryWhen(new Reconnector<>(strategy))
		         .repeatWhen(new Reconnector<>(strategy))
		         .subscribe();

		return new MonoToListenableFutureAdapter<>(promise);
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		if (stopping) {
			return new MonoToListenableFutureAdapter<>(Mono.empty());
		}

		stopping = true;

		Mono<Void> closing = ChannelFutureMono.from(group.close());

		if (configuration.scheduler != null) {
			closing =
					closing.doAfterTerminate((x, e) -> configuration.scheduler.shutdown());
		}

		return new MonoToListenableFutureAdapter<>(closing);
	}

	/**
	 * A configuration holder
	 */
	public static final class MessageHandlerConfiguration<P> {

		private final Function<? super ByteBuf, ? extends Collection<Message<P>>> decoder;
		private final BiConsumer<? super ByteBuf, ? super Message<P>>             encoder;
		private final int                                                         backlog;
		private final Scheduler
		                                                                          scheduler;

		public MessageHandlerConfiguration(Function<? super ByteBuf, ? extends Collection<Message<P>>> decoder,
				BiConsumer<? super ByteBuf, ? super Message<P>> encoder,
				int backlog,
				Scheduler scheduler) {
			this.decoder = decoder;
			this.encoder = encoder;
			this.backlog = backlog > 0 ? backlog : QueueSupplier.SMALL_BUFFER_SIZE;
			this.scheduler = scheduler;
		}
	}

	private static final class MessageHandler<P> implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

		private final TcpConnectionHandler<P> connectionHandler;

		private final MessageHandlerConfiguration<P> configuration;

		MessageHandler(TcpConnectionHandler<P> connectionHandler,
				MessageHandlerConfiguration<P> configuration) {
			this.connectionHandler = connectionHandler;
			this.configuration = configuration;
		}

		@Override
		public Publisher<Void> apply(NettyInbound in, NettyOutbound out) {
			Flux<Collection<Message<P>>> inbound = in.receive()
			                                         .map(configuration.decoder);

			DirectProcessor<Void> promise = DirectProcessor.create();
			TcpConnection<P> tcpConnection = new ReactorNettyTcpConnection<>(in,
					out,
					configuration.encoder,
					promise);

			if (configuration.scheduler != null) {
				configuration.scheduler.schedule(() -> connectionHandler.afterConnected(
						tcpConnection));
				inbound =
						inbound.publishOn(configuration.scheduler, configuration.backlog);
			}
			else {
				connectionHandler.afterConnected(tcpConnection);
			}

			inbound.flatMapIterable(Function.identity())
			       .subscribe(connectionHandler::handleMessage,
					       connectionHandler::handleFailure,
					       connectionHandler::afterConnectionClosed);

			return promise;
		}

	}

	static final class Reconnector<T> implements Function<Flux<T>, Publisher<?>> {

		private final ReconnectStrategy strategy;

		Reconnector(ReconnectStrategy strategy) {
			this.strategy = strategy;
		}

		@Override
		public Publisher<?> apply(Flux<T> flux) {
			return flux.scan(1, (p, e) -> p++)
			           .flatMap(attempt -> Mono.delayMillis(strategy.getTimeToNextAttempt(
					           attempt)));
		}
	}

}
