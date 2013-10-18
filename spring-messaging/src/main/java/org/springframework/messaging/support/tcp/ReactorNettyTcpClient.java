/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support.tcp;

import java.net.InetSocketAddress;

import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import reactor.core.Environment;
import reactor.core.composable.Composable;
import reactor.core.composable.Promise;
import reactor.function.Consumer;
import reactor.io.Buffer;
import reactor.tcp.Reconnect;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.encoding.Codec;
import reactor.tcp.netty.NettyTcpClient;
import reactor.tcp.spec.TcpClientSpec;
import reactor.tuple.Tuple;
import reactor.tuple.Tuple2;

/**
 * A Reactor/Netty implementation of {@link TcpOperations}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReactorNettyTcpClient<P> implements TcpOperations<P> {

	private Environment environment;

	private TcpClient<Message<P>, Message<P>> tcpClient;


	public ReactorNettyTcpClient(String host, int port, Codec<Buffer, Message<P>, Message<P>> codec) {
		this.environment = new Environment();
		this.tcpClient = new TcpClientSpec<Message<P>, Message<P>>(NettyTcpClient.class)
				.env(this.environment)
				.codec(codec)
				.connect(host, port)
				.get();
	}


	@Override
	public void connect(TcpConnectionHandler<P> connectionHandler) {
		this.connect(connectionHandler, null);
	}

	@Override
	public void connect(final TcpConnectionHandler<P> connectionHandler,
			final ReconnectStrategy reconnectStrategy) {

		Composable<TcpConnection<Message<P>, Message<P>>> composable;

		if (reconnectStrategy != null) {
			composable = this.tcpClient.open(new Reconnect() {
				@Override
				public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
					return Tuple.of(address, reconnectStrategy.getTimeToNextAttempt(attempt));
				}
			});
		}
		else {
			composable = this.tcpClient.open();
		}

		composable.when(Throwable.class, new Consumer<Throwable>() {
			@Override
			public void accept(Throwable ex) {
				connectionHandler.afterConnectFailure(ex);
			}
		});

		composable.consume(new Consumer<TcpConnection<Message<P>, Message<P>>>() {
			@Override
			public void accept(TcpConnection<Message<P>, Message<P>> connection) {
				connection.on().close(new Runnable() {
					@Override
					public void run() {
						connectionHandler.afterConnectionClosed();
					}
				});
				connection.in().consume(new Consumer<Message<P>>() {
					@Override
					public void accept(Message<P> message) {
						connectionHandler.handleMessage(message);
					}
				});
				connectionHandler.afterConnected(new ReactorTcpConnection<P>(connection));
			}
		});
	}

	@Override
	public ListenableFuture<Void> shutdown() {
		try {
			Promise<Void> promise = this.tcpClient.close();
			return new PromiseToListenableFutureAdapter<Void, Void>(promise) {
				@Override
				protected Void adapt(Void result) {
					return result;
				}
			};
		}
		finally {
			this.environment.shutdown();
		}
	}

}
