/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Arrays;
import java.util.Properties;

import reactor.core.Environment;
import reactor.core.composable.Composable;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Promises;
import reactor.core.configuration.ConfigurationReader;
import reactor.core.configuration.DispatcherConfiguration;
import reactor.core.configuration.ReactorConfiguration;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;
import reactor.io.encoding.Codec;
import reactor.net.NetChannel;
import reactor.net.Reconnect;
import reactor.net.netty.tcp.NettyTcpClient;
import reactor.net.tcp.TcpClient;
import reactor.net.tcp.spec.TcpClientSpec;
import reactor.tuple.Tuple;
import reactor.tuple.Tuple2;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpOperations}
 * based on the TCP client support of the Reactor project.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class Reactor11TcpClient<P> implements TcpOperations<P> {

	@SuppressWarnings("rawtypes")
	public static final Class<NettyTcpClient> REACTOR_TCP_CLIENT_TYPE = NettyTcpClient.class;


	private final TcpClient<Message<P>, Message<P>> tcpClient;

	private final Environment environment;


	/**
	 * A constructor that creates a {@link reactor.net.netty.tcp.NettyTcpClient} with
	 * a {@link reactor.event.dispatch.SynchronousDispatcher} as a result of which
	 * network I/O is handled in Netty threads.
	 * <p>Also see the constructor accepting a pre-configured Reactor
	 * {@link reactor.net.tcp.TcpClient}.
	 * @param host the host to connect to
	 * @param port the port to connect to
	 * @param codec the codec to use for encoding and decoding the TCP stream
	 */
	public Reactor11TcpClient(String host, int port, Codec<Buffer, Message<P>, Message<P>> codec) {
		this.environment = new Environment(new SynchronousDispatcherConfigReader());
		this.tcpClient = new TcpClientSpec<Message<P>, Message<P>>(REACTOR_TCP_CLIENT_TYPE)
				.env(this.environment).codec(codec).connect(host, port).get();
	}

	/**
	 * A constructor with a pre-configured {@link reactor.net.tcp.TcpClient}.
	 * <p><strong>NOTE:</strong> if the client is configured with a thread-creating
	 * dispatcher, you are responsible for shutting down the {@link reactor.core.Environment}
	 * instance with which the client is configured.
	 * @param tcpClient the TcpClient to use
	 */
	public Reactor11TcpClient(TcpClient<Message<P>, Message<P>> tcpClient) {
		Assert.notNull(tcpClient, "'tcpClient' must not be null");
		this.tcpClient = tcpClient;
		this.environment = null;
	}


	@Override
	public ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler) {
		Promise<NetChannel<Message<P>, Message<P>>> promise = this.tcpClient.open();
		composeConnectionHandling(promise, connectionHandler);

		return new AbstractPromiseToListenableFutureAdapter<NetChannel<Message<P>, Message<P>>, Void>(promise) {
			@Override
			protected Void adapt(NetChannel<Message<P>, Message<P>> result) {
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

		Stream<NetChannel<Message<P>, Message<P>>> stream = this.tcpClient.open(reconnect);
		composeConnectionHandling(stream, connectionHandler);

		Promise<Void> promise = Promises.next(stream).map(
				new Function<NetChannel<Message<P>, Message<P>>, Void>() {
					@Override
					public Void apply(NetChannel<Message<P>, Message<P>> ch) {
						return null;
					}
				});
		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}

	private void composeConnectionHandling(Composable<NetChannel<Message<P>, Message<P>>> composable,
			final TcpConnectionHandler<P> connectionHandler) {

		composable
				.when(Throwable.class, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable ex) {
						connectionHandler.afterConnectFailure(ex);
					}
				})
				.consume(new Consumer<NetChannel<Message<P>, Message<P>>>() {
					@Override
					public void accept(NetChannel<Message<P>, Message<P>> connection) {
						connection
								.when(Throwable.class, new Consumer<Throwable>() {
									@Override
									public void accept(Throwable ex) {
										connectionHandler.handleFailure(ex);
									}
								})
								.consume(new Consumer<Message<P>>() {
									@Override
									public void accept(Message<P> message) {
										connectionHandler.handleMessage(message);
									}
								})
								.on()
								.close(new Runnable() {
									@Override
									public void run() {
										connectionHandler.afterConnectionClosed();
									}
								});
						connectionHandler.afterConnected(new Reactor11TcpConnection<P>(connection));
					}
				});
	}

	@Override
	public ListenableFuture<Boolean> shutdown() {
		try {
			Promise<Boolean> promise = this.tcpClient.close();
			return new AbstractPromiseToListenableFutureAdapter<Boolean, Boolean>(promise) {
				@Override
				protected Boolean adapt(Boolean result) {
					return result;
				}
			};
		}
		finally {
			this.environment.shutdown();

		}
	}


	/**
	 * A ConfigurationReader that enforces the use of a SynchronousDispatcher.
	 * <p>The {@link reactor.core.configuration.PropertiesConfigurationReader} used by
	 * default automatically creates other dispatchers with thread pools that are
	 * not needed.
	 */
	private static class SynchronousDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			return new ReactorConfiguration(Arrays.<DispatcherConfiguration>asList(), "sync", new Properties());
		}
	}

}