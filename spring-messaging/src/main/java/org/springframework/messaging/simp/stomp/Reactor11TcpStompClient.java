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
package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.Properties;

import reactor.core.Environment;
import reactor.core.configuration.ConfigurationReader;
import reactor.core.configuration.DispatcherConfiguration;
import reactor.core.configuration.DispatcherType;
import reactor.core.configuration.ReactorConfiguration;
import reactor.net.netty.tcp.NettyTcpClient;
import reactor.net.tcp.TcpClient;
import reactor.net.tcp.spec.TcpClientSpec;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor11TcpClient;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * A STOMP over TCP client that uses
 * {@link org.springframework.messaging.tcp.reactor.Reactor11TcpClient
 * Reactor11TcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class Reactor11TcpStompClient extends StompClientSupport {

	private final TcpOperations<byte[]> tcpClient;


	/**
	 * Create an instance with host "127.0.0.1" and port 61613.
	 */
	public Reactor11TcpStompClient() {
		this("127.0.0.1", 61613);
	}

	/**
	 * Create an instance with the given host and port.
	 * @param host the host
	 * @param port the port
	 */
	public Reactor11TcpStompClient(String host, int port) {
		this.tcpClient = new Reactor11TcpClient<byte[]>(createNettyTcpClient(host, port));
	}

	private TcpClient<Message<byte[]>, Message<byte[]>> createNettyTcpClient(String host, int port) {
		return new TcpClientSpec<Message<byte[]>, Message<byte[]>>(NettyTcpClient.class)
				.env(new Environment(new StompClientDispatcherConfigReader()))
				.codec(new Reactor11StompCodec(new StompEncoder(), new StompDecoder()))
				.connect(host, port)
				.get();
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public Reactor11TcpStompClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}


	/**
	 * Connect and notify the given {@link StompSessionHandler} when connected
	 * on the STOMP level,
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


	/**
	 * A ConfigurationReader with a thread pool-based dispatcher.
	 */
	private static class StompClientDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			String dispatcherName = "StompClient";
			DispatcherType dispatcherType = DispatcherType.THREAD_POOL_EXECUTOR;
			DispatcherConfiguration config = new DispatcherConfiguration(dispatcherName, dispatcherType, 128, 0);
			return new ReactorConfiguration(Arrays.<DispatcherConfiguration>asList(config), dispatcherName, new Properties());
		}
	}

}
