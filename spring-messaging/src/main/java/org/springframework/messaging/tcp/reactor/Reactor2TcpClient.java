/*
 * Copyright 2002-2014 the original author or authors.
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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.netty.channel.nio.NioEventLoopGroup;
import org.reactivestreams.Publisher;
import reactor.Environment;
import reactor.core.config.ConfigurationReader;
import reactor.core.config.DispatcherConfiguration;
import reactor.core.config.ReactorConfiguration;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.fn.BiFunction;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;
import reactor.io.net.*;
import reactor.io.net.Spec.TcpClientSpec;
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
public class Reactor2TcpClient<P> implements TcpOperations<P> {

	@SuppressWarnings("rawtypes")
	public static final Class<NettyTcpClient> REACTOR_TCP_CLIENT_TYPE = NettyTcpClient.class;

	private final NetStreams.TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory;

	private final List<TcpClient<Message<P>, Message<P>>> tcpClients =
			new ArrayList<TcpClient<Message<P>, Message<P>>>();


	/**
	 * A constructor that creates a {@link TcpClientSpec TcpClientSpec} factory
	 * with a default {@link reactor.core.dispatch.SynchronousDispatcher}, i.e.
	 * relying on Netty threads. The number of Netty threads can be tweaked with
	 * the {@code reactor.tcp.ioThreadCount} System property. The network I/O
	 * threads will be shared amongst the active clients.
	 * <p>Also see the constructor accepting a ready Reactor
	 * {@link TcpClientSpec} {@link Function} factory.
	 *
	 * @param host  the host to connect to
	 * @param port  the port to connect to
	 * @param codec the codec to use for encoding and decoding the TCP stream
	 */
	public Reactor2TcpClient(final String host, final int port, final Codec<Buffer, Message<P>, Message<P>> codec) {

		final NioEventLoopGroup eventLoopGroup = initEventLoopGroup();

		this.tcpClientSpecFactory = new NetStreams.TcpClientFactory<Message<P>, Message<P>>() {

			@Override
			public TcpClientSpec<Message<P>, Message<P>> apply(TcpClientSpec<Message<P>, Message<P>> spec) {
				return spec
						.env(new Environment(new SynchronousDispatcherConfigReader()))
						.codec(codec)
						.connect(host, port)
						.options(new NettyClientSocketOptions().eventLoopGroup(eventLoopGroup));
			}
		};
	}

	private NioEventLoopGroup initEventLoopGroup() {
		int ioThreadCount;
		try {
			ioThreadCount = Integer.parseInt(System.getProperty("reactor.tcp.ioThreadCount"));
		} catch (Exception i) {
			ioThreadCount = -1;
		}
		if (ioThreadCount <= 0l) {
			ioThreadCount = Runtime.getRuntime().availableProcessors();
		}

		return new NioEventLoopGroup(ioThreadCount,
				new NamedDaemonThreadFactory("reactor-tcp-io"));
	}

	/**
	 * A constructor with a pre-configured {@link TcpClientSpec} {@link Function}
	 * factory. This might be used to add SSL or specific network parameters to
	 * the generated client configuration.
	 * <p><strong>NOTE:</strong> if the client is configured with a thread-creating
	 * dispatcher, you are responsible for cleaning them, e.g. using
	 * {@link reactor.core.Dispatcher#shutdown}.
	 *
	 * @param tcpClientSpecFactory the TcpClientSpec {@link Function} to use for each client creation.
	 */
	public Reactor2TcpClient(NetStreams.TcpClientFactory<Message<P>, Message<P>> tcpClientSpecFactory) {
		Assert.notNull(tcpClientSpecFactory, "'tcpClientClientFactory' must not be null");
		this.tcpClientSpecFactory = tcpClientSpecFactory;
	}


	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler) {
		Class<NettyTcpClient> type = REACTOR_TCP_CLIENT_TYPE;

		TcpClient<Message<P>, Message<P>> tcpClient = NetStreams.tcpClient(type, this.tcpClientSpecFactory);

		Promise<Void> promise = tcpClient.start(composeConnectionHandling(tcpClient, connectionHandler));

		return new PassThroughPromiseToListenableFutureAdapter<Void>(
				promise.onError(new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) {
						connectionHandler.afterConnectFailure(throwable);
					}
				})
		);
	}

	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> handler, ReconnectStrategy strategy) {
		Assert.notNull(strategy, "ReconnectStrategy must not be null");
		Class<NettyTcpClient> type = REACTOR_TCP_CLIENT_TYPE;

		TcpClient<Message<P>, Message<P>> tcpClient = NetStreams.tcpClient(type, this.tcpClientSpecFactory);

		Stream<Tuple2<InetSocketAddress, Integer>> stream = tcpClient.start(
				composeConnectionHandling(tcpClient, handler),
				new ReactorRectonnectAdapter(strategy)
		);

		return new PassThroughPromiseToListenableFutureAdapter<Void>(stream.next().after());
	}

	private MessageHandler<P> composeConnectionHandling(
			final TcpClient<Message<P>, Message<P>> tcpClient,
			final TcpConnectionHandler<P> connectionHandler
	) {

		synchronized (this.tcpClients) {
			this.tcpClients.add(tcpClient);
		}

		return new MessageHandler<P>() {
			@Override
			public Publisher<Void> apply(ChannelStream<Message<P>, Message<P>> connection) {

				Promise<Void> closePromise = Promises.prepare();

				connectionHandler.afterConnected(new Reactor2TcpConnection<P>(connection, closePromise));

				connection
						.finallyDo(new Consumer<Signal<Message<P>>>() {

							@Override
							public void accept(Signal<Message<P>> signal) {
								if (signal.isOnError()) {
									connectionHandler.handleFailure(signal.getThrowable());
								} else if (signal.isOnComplete()) {
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
		};
	}

	@Override
	public ListenableFuture<Void> shutdown() {

		final List<TcpClient<Message<P>, Message<P>>> clients;

		synchronized (this.tcpClients) {
			clients = new ArrayList<TcpClient<Message<P>, Message<P>>>(this.tcpClients);
		}

		Promise<Void> promise = Streams.from(clients)
				.flatMap(new Function<TcpClient<Message<P>, Message<P>>, Promise<Void>>() {
					@Override
					public Promise<Void> apply(final TcpClient<Message<P>, Message<P>> client) {
						return client.shutdown().onComplete(new Consumer<Promise<Void>>() {
							@Override
							public void accept(Promise<Void> voidPromise) {
								synchronized (tcpClients) {
									tcpClients.remove(client);
								}
							}
						});
					}
				})
				.next();

		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}

	private static class SynchronousDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			return new ReactorConfiguration(Arrays.<DispatcherConfiguration>asList(), "sync", new Properties());
		}
	}

	private static class ReactorRectonnectAdapter implements Reconnect {

		private final ReconnectStrategy strategy;

		public ReactorRectonnectAdapter(ReconnectStrategy strategy) {
			this.strategy = strategy;
		}

		@Override
		public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
			return Tuple.of(address, strategy.getTimeToNextAttempt(attempt));
		}

	}

	private interface MessageHandler<P>
			extends ReactorChannelHandler<Message<P>, Message<P>, ChannelStream<Message<P>, Message<P>>>{
	}

}
