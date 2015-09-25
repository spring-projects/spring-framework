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
import java.util.List;
import java.util.Properties;

import reactor.Environment;
import reactor.core.config.ConfigurationReader;
import reactor.core.config.DispatcherConfiguration;
import reactor.core.config.DispatcherType;
import reactor.core.config.ReactorConfiguration;
import reactor.io.net.NetStreams;
import reactor.io.net.Spec.TcpClientSpec;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A STOMP over TCP client that uses
 * {@link Reactor2TcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class Reactor2TcpStompClient extends StompClientSupport {

	private final TcpOperations<byte[]> tcpClient;


	/**
	 * Create an instance with host "127.0.0.1" and port 61613.
	 */
	public Reactor2TcpStompClient() {
		this("127.0.0.1", 61613);
	}

	/**
	 * Create an instance with the given host and port.
	 * @param host the host
	 * @param port the port
	 */
	public Reactor2TcpStompClient(final String host, final int port) {
		ConfigurationReader reader = new StompClientDispatcherConfigReader();
		Environment environment = new Environment(reader).assignErrorJournal();
		StompTcpClientSpecFactory factory = new StompTcpClientSpecFactory(environment, host, port);
		this.tcpClient = new Reactor2TcpClient<byte[]>(factory);
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public Reactor2TcpStompClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
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


	/**
	 * A ConfigurationReader with a thread pool-based dispatcher.
	 */
	private static class StompClientDispatcherConfigReader implements ConfigurationReader {

		@Override
		public ReactorConfiguration read() {
			String dispatcherName = "StompClient";
			DispatcherType dispatcherType = DispatcherType.DISPATCHER_GROUP;
			DispatcherConfiguration config = new DispatcherConfiguration(dispatcherName, dispatcherType, 128, 0);
			List<DispatcherConfiguration> configList = Arrays.<DispatcherConfiguration>asList(config);
			return new ReactorConfiguration(configList, dispatcherName, new Properties());
		}
	}


	private static class StompTcpClientSpecFactory
			implements NetStreams.TcpClientFactory<Message<byte[]>, Message<byte[]>> {

		private final Environment environment;

		private final String host;

		private final int port;

		public StompTcpClientSpecFactory(Environment environment, String host, int port) {
			this.environment = environment;
			this.host = host;
			this.port = port;
		}

		@Override
		public TcpClientSpec<Message<byte[]>, Message<byte[]>> apply(
				TcpClientSpec<Message<byte[]>, Message<byte[]>> tcpClientSpec) {

			return tcpClientSpec
					.codec(new Reactor2StompCodec(new StompEncoder(), new StompDecoder()))
					.env(this.environment)
					.dispatcher(this.environment.getCachedDispatchers("StompClient").get())
					.connect(this.host, this.port);
		}
	}

}
