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

package org.springframework.messaging.simp.stomp;

import io.netty.channel.EventLoopGroup;
import reactor.Environment;
import reactor.io.net.NetStreams.TcpClientFactory;
import reactor.io.net.Spec.TcpClientSpec;
import reactor.io.net.impl.netty.NettyClientSocketOptions;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A STOMP over TCP client that uses {@link Reactor2TcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class Reactor2TcpStompClient extends StompClientSupport implements Lifecycle {

	private final TcpOperations<byte[]> tcpClient;

	private final EventLoopGroup eventLoopGroup;

	private final Environment environment;

	private volatile boolean running = false;


	/**
	 * Create an instance with host "127.0.0.1" and port 61613.
	 */
	public Reactor2TcpStompClient() {
		this("127.0.0.1", 61613);
	}

	/**
	 * Create an instance with the given host and port to connect to
	 */
	public Reactor2TcpStompClient(String host, int port) {
		this.eventLoopGroup = Reactor2TcpClient.initEventLoopGroup();
		this.environment = new Environment();
		this.tcpClient = new Reactor2TcpClient<byte[]>(
				new StompTcpClientSpecFactory(host, port, this.eventLoopGroup, this.environment));
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public Reactor2TcpStompClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
		this.eventLoopGroup = null;
		this.environment = null;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			try {
				if (this.eventLoopGroup != null) {
					this.eventLoopGroup.shutdownGracefully().await(5000);
				}
				if (this.environment != null) {
					this.environment.shutdown();
				}
			}
			catch (InterruptedException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to shutdown gracefully", ex);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	/**
	 * Connect and notify the given {@link StompSessionHandler} when connected
	 * on the STOMP level.
	 * @param handler the handler for the STOMP session
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(StompSessionHandler handler) {
		return connect(null, handler);
	}

	/**
	 * An overloaded version of {@link #connect(StompSessionHandler)} that
	 * accepts headers to use for the STOMP CONNECT frame.
	 * @param connectHeaders headers to add to the CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connect(session);
		return session.getSessionFuture();
	}

	/**
	 * Shut down the client and release resources.
	 */
	public void shutdown() {
		this.tcpClient.shutdown();
	}


	private static class StompTcpClientSpecFactory implements TcpClientFactory<Message<byte[]>, Message<byte[]>> {

		private final String host;

		private final int port;

		private final NettyClientSocketOptions socketOptions;

		private final Environment environment;

		private final Reactor2StompCodec codec;


		StompTcpClientSpecFactory(String host, int port, EventLoopGroup group, Environment environment) {
			this.host = host;
			this.port = port;
			this.socketOptions = new NettyClientSocketOptions().eventLoopGroup(group);
			this.environment = environment;
			this.codec = new Reactor2StompCodec(new StompEncoder(), new StompDecoder());
		}

		@Override
		public TcpClientSpec<Message<byte[]>, Message<byte[]>> apply(
				TcpClientSpec<Message<byte[]>, Message<byte[]>> clientSpec) {

			return clientSpec
					.env(this.environment)
					.dispatcher(this.environment.getDispatcher(Environment.WORK_QUEUE))
					.connect(this.host, this.port)
					.codec(this.codec)
					.options(this.socketOptions);
		}
	}

}
