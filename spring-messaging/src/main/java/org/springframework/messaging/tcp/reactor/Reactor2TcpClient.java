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

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.fn.BiFunction;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.io.buffer.Buffer;
import reactor.io.codec.Codec;
import reactor.io.net.ChannelStream;
import reactor.io.net.NetStreams;
import reactor.io.net.Reconnect;
import reactor.io.net.Spec;
import reactor.io.net.impl.netty.NettyClientSocketOptions;
import reactor.io.net.impl.netty.tcp.NettyTcpClient;
import reactor.io.net.tcp.TcpClient;
import reactor.rx.Promise;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpOperations}
 * based on the TCP client support of the Reactor project.
 * <p/>
 * This client will wrap N number of clients for N {@link #connect} calls (one client by connection).
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 4.2
 */
public class Reactor2TcpClient<P> implements TcpOperations<P> {

	@SuppressWarnings("rawtypes")
	public static final Class<NettyTcpClient> REACTOR_TCP_CLIENT_TYPE = NettyTcpClient.class;

	private final Function<Spec.TcpClientSpec<Message<P>, Message<P>>,
			Spec.TcpClientSpec<Message<P>, Message<P>>> tcpClientSpec;

	private final List<TcpClient<Message<P>, Message<P>>> activeClients =
			new ArrayList<TcpClient<Message<P>, Message<P>>>();

	/**
	 * A constructor that creates a {@link reactor.io.net.Spec.TcpClientSpec} factory with
	 * a default {@link reactor.core.dispatch.SynchronousDispatcher} as a result of which
	 * network I/O is handled in Netty threads. Number of Netty threads can be tweaked with
	 * the {@code reactor.tcp.ioThreadCount} System property.
	 * <p>
	 * The network I/O threads will be shared amongst the active clients.
	 * </p>
	 * <p/>
	 * <p>Also see the constructor accepting a ready Reactor
	 * {@link reactor.io.net.Spec.TcpClientSpec} {@link Function} factory.
	 *
	 * @param host  the host to connect to
	 * @param port  the port to connect to
	 * @param codec the codec to use for encoding and decoding the TCP stream
	 */
	public Reactor2TcpClient(final String host, final int port, final Codec<Buffer, Message<P>, Message<P>> codec) {

		//FIXME Should it be exposed in Spring ?
		int ioThreadCount;
		try {
			ioThreadCount = Integer.parseInt(System.getProperty("reactor.tcp.ioThreadCount"));
		} catch (Exception i) {
			ioThreadCount = -1;
		}
		if (ioThreadCount <= 0l) {
			ioThreadCount = Runtime.getRuntime().availableProcessors();
		}

		final NioEventLoopGroup eventLoopGroup =
				new NioEventLoopGroup(ioThreadCount, new NamedDaemonThreadFactory("reactor-tcp-io"));

		this.tcpClientSpec = new Function<Spec.TcpClientSpec<Message<P>, Message<P>>,
				Spec.TcpClientSpec<Message<P>, Message<P>>>() {

			@Override
			public Spec.TcpClientSpec<Message<P>, Message<P>> apply(Spec.TcpClientSpec<Message<P>, Message<P>>
					                                                        messageMessageTcpClientSpec) {
				return messageMessageTcpClientSpec
						.codec(codec)
								//make connect dynamic or use reconnect strategy to LB onto cluster
						.connect(host, port)
						.options(new NettyClientSocketOptions().eventLoopGroup(eventLoopGroup));
			}
		};
	}

	/**
	 * A constructor with a pre-configured {@link reactor.io.net.Spec.TcpClientSpec} {@link Function} factory.
	 * This might be used to add SSL or specific network parameters to the generated client configuration.
	 * <p/>
	 * <p><strong>NOTE:</strong> if the client is configured with a thread-creating
	 * dispatcher, you are responsible for cleaning them, e.g. using {@link reactor.core.Dispatcher#shutdown}.
	 *
	 * @param tcpClientSpecFactory the TcpClientSpec {@link Function} to use for each client creation.
	 */
	public Reactor2TcpClient(Function<Spec.TcpClientSpec<Message<P>, Message<P>>,
			Spec.TcpClientSpec<Message<P>, Message<P>>> tcpClientSpecFactory) {
		Assert.notNull(tcpClientSpecFactory, "'tcpClientClientFactory' must not be null");
		this.tcpClientSpec = tcpClientSpecFactory;
	}


	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler) {

		//create a new client
		TcpClient<Message<P>, Message<P>> tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, tcpClientSpec);

		//attach connection handler
		composeConnectionHandling(tcpClient, connectionHandler);

		//open connection
		Promise<Boolean> promise = tcpClient.open();

		//adapt to ListenableFuture
		return new AbstractPromiseToListenableFutureAdapter<Boolean, Void>(promise) {
			@Override
			protected Void adapt(Boolean result) {
				return null;
			}
		};
	}

	@Override
	public ListenableFuture<Void> connect(final TcpConnectionHandler<P> connectionHandler,
	                                      final ReconnectStrategy reconnectStrategy) {

		Assert.notNull(reconnectStrategy, "ReconnectStrategy must not be null");

		Reconnect reconnect = new Reconnect() {
			@Override
			public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
				return Tuple.of(address, reconnectStrategy.getTimeToNextAttempt(attempt));
			}
		};

		//create a new client
		TcpClient<Message<P>, Message<P>> tcpClient = NetStreams.tcpClient(REACTOR_TCP_CLIENT_TYPE, tcpClientSpec);

		//attach connection handler
		composeConnectionHandling(tcpClient, connectionHandler);

		//open connection
		Stream<Boolean> stream = tcpClient.open(reconnect);

		//adapt to ListenableFuture with the next available connection
		Promise<Void> promise = stream.next().map(
				new Function<Boolean, Void>() {
					@Override
					public Void apply(Boolean ch) {
						return null;
					}
				});

		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}

	private void composeConnectionHandling(final TcpClient<Message<P>, Message<P>> tcpClient,
	                                       final TcpConnectionHandler<P> connectionHandler) {

		synchronized (activeClients){
			activeClients.add(tcpClient);
		}

		tcpClient
				.finallyDo(new Consumer<Signal<ChannelStream<Message<P>,Message<P>>>>() {
					@Override
					public void accept(Signal<ChannelStream<Message<P>,Message<P>>> signal) {
						synchronized (activeClients) {
							activeClients.remove(tcpClient);
						}
						if(signal.isOnError()) {
							connectionHandler.afterConnectFailure(signal.getThrowable());
						}
					}
				})
				.log("reactor.client")
				.consume(new Consumer<ChannelStream<Message<P>, Message<P>>>() {
					@Override
					public void accept(ChannelStream<Message<P>, Message<P>> connection) {
						connection
								.log("reactor.connection")
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

						connectionHandler.afterConnected(new Reactor2TcpConnection<P>(connection));
					}
				});
	}

	@Override
	public ListenableFuture<Boolean> shutdown() {
		final List<TcpClient<Message<P>, Message<P>>> clients;

		synchronized (activeClients){
			clients = new ArrayList<TcpClient<Message<P>, Message<P>>>(activeClients);
			//should be cleared individually in tcpClient#finallyDo()
			//activeClients.clear();
		}

		Promise<Boolean> promise = Streams.from(clients)
				.flatMap(new Function<TcpClient<Message<P>, Message<P>>, Promise<Boolean>>() {
					@Override
					public Promise<Boolean> apply(TcpClient<Message<P>, Message<P>> client) {
						return client.close();
					}
				})
				.reduce(new BiFunction<Boolean, Boolean, Boolean>() {
					@Override
					public Boolean apply(Boolean prev, Boolean next) {
						return prev && next;
					}
				})
				.next();

		return new AbstractPromiseToListenableFutureAdapter<Boolean, Boolean>(promise) {
			@Override
			protected Boolean adapt(Boolean result) {
				return result;
			}
		};
	}
}