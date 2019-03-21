/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.reactivestreams.Publisher;
import reactor.Environment;
import reactor.core.config.ConfigurationReader;
import reactor.core.config.DispatcherConfiguration;
import reactor.core.config.ReactorConfiguration;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.Supplier;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;
import reactor.io.net.ChannelStream;
import reactor.io.net.NetStreams;
import reactor.io.net.NetStreams.TcpClientFactory;
import reactor.io.net.ReactorChannelHandler;
import reactor.io.net.Reconnect;
import reactor.io.net.Spec.TcpClientSpec;
import reactor.io.net.config.ClientSocketOptions;
import reactor.io.net.impl.netty.NettyClientSocketOptions;
import reactor.io.net.impl.netty.tcp.NettyTcpClient;
import reactor.io.net.tcp.TcpClient;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpOperations}
 * based on the TCP client support of project Reactor.
 *
 * <p>This implementation wraps N Reactor {@code TcpClient} instances created
 * for N {@link #connect} calls, i.e. once instance per connection.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 4.2
 */
public class Reactor2TcpClient<P> implements TcpOperations<P> {

	@SuppressWarnings("rawtypes")
	public static final Class<NettyTcpClient> REACTOR_TCP_CLIENT_TYPE = NettyTcpClient.class;

	private static final Method eventLoopGroupMethod = initEventLoopGroupMethod();


	private final EventLoopGroup eventLoopGroup;

	private final Environment environment;

	private final TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory;

	private final List<TcpClient<Message<P>, Message<P>>> tcpClients =
			new ArrayList<TcpClient<Message<P>, Message<P>>>();

	private boolean stopping;


	/**
	 * A constructor that creates a {@link TcpClientSpec TcpClientSpec} factory
	 * with a default {@link reactor.core.dispatch.SynchronousDispatcher}, i.e.
	 * relying on Netty threads. The number of Netty threads can be tweaked with
	 * the {@code reactor.tcp.ioThreadCount} System property. The network I/O
	 * threads will be shared amongst the active clients.
	 *
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param codec the codec to use for encoding and decoding the TCP stream
	 */
	public Reactor2TcpClient(final String host, final int port, final Codec<Buffer, Message<P>, Message<P>> codec) {
		this(new FixedAddressSupplier(host, port), codec);
	}

	/**
	 * A variant of {@link #Reactor2TcpClient(String, int, Codec)} that takes a
	 * supplier of any number of addresses instead of just one host and port.
	 * This can be used to {@link #connect(TcpConnectionHandler, ReconnectStrategy)
	 * reconnect} to a different address after the current host becomes unavailable.
	 *
	 * @param addressSupplier supplier of addresses to use for connecting
	 * @param codec the codec to use for encoding and decoding the TCP stream
	 * @since 4.3.15
	 */
	public Reactor2TcpClient(final Supplier<InetSocketAddress> addressSupplier,
			final Codec<Buffer, Message<P>, Message<P>> codec) {

		// Reactor 2.0.5 requires NioEventLoopGroup vs 2.0.6+ requires EventLoopGroup
		final NioEventLoopGroup nioEventLoopGroup = initEventLoopGroup();
		this.eventLoopGroup = nioEventLoopGroup;
		this.environment = new Environment(new SynchronousDispatcherConfigReader());

		this.tcpClientSpecFactory = new TcpClientFactory<Message<P>, Message<P>>() {
			@Override
			public TcpClientSpec<Message<P>, Message<P>> apply(TcpClientSpec<Message<P>, Message<P>> spec) {
				return spec
						.env(environment)
						.codec(codec)
						.connect(addressSupplier)
						.options(createClientSocketOptions());
			}

			private ClientSocketOptions createClientSocketOptions() {
				return (ClientSocketOptions) ReflectionUtils.invokeMethod(eventLoopGroupMethod,
						new NettyClientSocketOptions(), nioEventLoopGroup);
			}
		};
	}

	/**
	 * A constructor with a pre-configured {@link TcpClientSpec} {@link Function}
	 * factory. This might be used to add SSL or specific network parameters to
	 * the generated client configuration.
	 *
	 * <p><strong>NOTE:</strong> if the client is configured with a thread-creating
	 * dispatcher, you are responsible for cleaning them, e.g. via
	 * {@link reactor.core.Dispatcher#shutdown}.
	 *
	 * @param tcpClientSpecFactory the TcpClientSpec {@link Function} to use for
	 * each client creation
	 */
	public Reactor2TcpClient(TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory) {
		Assert.notNull(tcpClientSpecFactory, "'tcpClientClientFactory' must not be null");
		this.tcpClientSpecFactory = tcpClientSpecFactory;
		this.eventLoopGroup = null;
		this.environment = null;
	}


	public static NioEventLoopGroup initEventLoopGroup() {
		int ioThreadCount;
		try {
			ioThreadCount = Integer.parseInt(System.getProperty("reactor.tcp.ioThreadCount"));
		}
		catch (Throwable ex) {
			ioThreadCount = -1;
		}
		if (ioThreadCount <= 0) {
			ioThreadCount = Runtime.getRuntime().availableProcessors();
		}
		return new NioEventLoopGroup(ioThreadCount, new NamedDaemonThreadFactory("reactor-tcp-io"));
	}


	@Override
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> connectionHandler) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");

		final TcpClient<Message<P>, Message<P>> tcpClient;
		final Runnable cleanupTask;
		synchronized (this.tcpClients) {
			if (this.stopping) {
				IllegalStateException ex = new IllegalStateException("Shutting down.");
				connectionHandler.afterConnectFailure(ex);
				return new PassThroughPromiseToListenableFutureAdapter<Void>(Promises.<Void>error(ex));
			}
			tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, this.tcpClientSpecFactory);
			this.tcpClients.add(tcpClient);
			cleanupTask = new Runnable() {
				@Override
				public void run() {
					synchronized (tcpClients) {
						tcpClients.remove(tcpClient);
					}
				}
			};
		}

		Promise<Void> promise = tcpClient.start(
				new MessageChannelStreamHandler<P>(connectionHandler, cleanupTask));

		return new PassThroughPromiseToListenableFutureAdapter<Void>(
				promise.onError(new Consumer<Throwable>() {
					@Override
					public void accept(Throwable ex) {
						cleanupTask.run();
						connectionHandler.afterConnectFailure(ex);
					}
				})
		);
	}

	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler, ReconnectStrategy strategy) {
		Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
		Assert.notNull(strategy, "ReconnectStrategy must not be null");

		final TcpClient<Message<P>, Message<P>> tcpClient;
		Runnable cleanupTask;
		synchronized (this.tcpClients) {
			if (this.stopping) {
				IllegalStateException ex = new IllegalStateException("Shutting down.");
				connectionHandler.afterConnectFailure(ex);
				return new PassThroughPromiseToListenableFutureAdapter<Void>(Promises.<Void>error(ex));
			}
			tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, this.tcpClientSpecFactory);
			this.tcpClients.add(tcpClient);
			cleanupTask = new Runnable() {
				@Override
				public void run() {
					synchronized (tcpClients) {
						tcpClients.remove(tcpClient);
					}
				}
			};
		}

		Stream<Tuple2<InetSocketAddress, Integer>> stream = tcpClient.start(
				new MessageChannelStreamHandler<P>(connectionHandler, cleanupTask),
				new ReactorReconnectAdapter(strategy));

		return new PassThroughPromiseToListenableFutureAdapter<Void>(stream.next().after());
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		synchronized (this.tcpClients) {
			this.stopping = true;
		}

		Promise<Void> promise = Streams.from(this.tcpClients)
				.flatMap(new Function<TcpClient<Message<P>, Message<P>>, Promise<Void>>() {
					@Override
					public Promise<Void> apply(final TcpClient<Message<P>, Message<P>> client) {
						return client.shutdown().onComplete(new Consumer<Promise<Void>>() {
							@Override
							public void accept(Promise<Void> voidPromise) {
								tcpClients.remove(client);
							}
						});
					}
				})
				.next();

		if (this.eventLoopGroup != null) {
			final Promise<Void> eventLoopPromise = Promises.prepare();
			promise.onComplete(new Consumer<Promise<Void>>() {
				@Override
				public void accept(Promise<Void> voidPromise) {
					eventLoopGroup.shutdownGracefully().addListener(new FutureListener<Object>() {
						@Override
						public void operationComplete(Future<Object> future) throws Exception {
							if (future.isSuccess()) {
								eventLoopPromise.onComplete();
							}
							else {
								eventLoopPromise.onError(future.cause());
							}
						}
					});
				}
			});
			promise = eventLoopPromise;
		}

		if (this.environment != null) {
			promise.onComplete(new Consumer<Promise<Void>>() {
				@Override
				public void accept(Promise<Void> voidPromise) {
					environment.shutdown();
				}
			});
		}

		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}


	private static Method initEventLoopGroupMethod() {
		for (Method method : NettyClientSocketOptions.class.getMethods()) {
			if (method.getName().equals("eventLoopGroup") && method.getParameterTypes().length == 1) {
				return method;
			}
		}
		throw new IllegalStateException("No compatible Reactor version found");
	}


	private static class FixedAddressSupplier implements Supplier<InetSocketAddress> {

		private final InetSocketAddress address;

		FixedAddressSupplier(String host, int port) {
			this.address = new InetSocketAddress(host, port);
		}

		@Override
		public InetSocketAddress get() {
			return this.address;
		}
	}


	private static class SynchronousDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			return new ReactorConfiguration(
					Collections.<DispatcherConfiguration>emptyList(), "sync", new Properties());
		}
	}


	private static class MessageChannelStreamHandler<P>
			implements ReactorChannelHandler<Message<P>, Message<P>, ChannelStream<Message<P>, Message<P>>> {

		private final TcpConnectionHandler<P> connectionHandler;

		private final Runnable cleanupTask;

		public MessageChannelStreamHandler(TcpConnectionHandler<P> connectionHandler, Runnable cleanupTask) {
			this.connectionHandler = connectionHandler;
			this.cleanupTask = cleanupTask;
		}

		@Override
		public Publisher<Void> apply(ChannelStream<Message<P>, Message<P>> channelStream) {
			Promise<Void> closePromise = Promises.prepare();
			this.connectionHandler.afterConnected(new Reactor2TcpConnection<P>(channelStream, closePromise));
			channelStream
					.finallyDo(new Consumer<Signal<Message<P>>>() {
						@Override
						public void accept(Signal<Message<P>> signal) {
							cleanupTask.run();
							if (signal.isOnError()) {
								connectionHandler.handleFailure(signal.getThrowable());
							}
							else if (signal.isOnComplete()) {
								connectionHandler.afterConnectionClosed();
							}
						}
					})
					.consume(new Consumer<Message<P>>() {
						@Override
						public void accept(Message<P> message) {
							connectionHandler.handleMessage(message);
						}
					});

			return closePromise;
		}
	}


	private static class ReactorReconnectAdapter implements Reconnect {

		private final ReconnectStrategy strategy;

		public ReactorReconnectAdapter(ReconnectStrategy strategy) {
			this.strategy = strategy;
		}

		@Override
		public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
			return Tuple.of(address, this.strategy.getTimeToNextAttempt(attempt));
		}
	}

}
