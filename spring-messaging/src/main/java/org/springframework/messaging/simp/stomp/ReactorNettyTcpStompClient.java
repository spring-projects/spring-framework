/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.Assert;

/**
 * A STOMP over TCP client, configurable with {@link ReactorNettyTcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyTcpStompClient extends StompClientSupport {

	private final TcpOperations<byte[]> tcpClient;


	/**
	 * Create an instance with host "127.0.0.1" and port 61613.
	 */
	public ReactorNettyTcpStompClient() {
		this("127.0.0.1", 61613);
	}

	/**
	 * Create an instance with the given host and port.
	 * @param host the host
	 * @param port the port
	 */
	public ReactorNettyTcpStompClient(String host, int port) {
		this.tcpClient = initTcpClient(host, port);
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public ReactorNettyTcpStompClient(TcpOperations<byte[]> tcpClient) {
		Assert.notNull(tcpClient, "'tcpClient' is required");
		this.tcpClient = tcpClient;
	}

	private static TcpOperations<byte[]> initTcpClient(String host, int port) {
		ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(host, port, new StompReactorNettyCodec());
		client.setLogger(SimpLogging.forLog(client.getLogger()));
		return client;
	}


	/**
	 * Connect and notify the given {@link StompSessionHandler} when connected
	 * on the STOMP level.
	 * @param handler the handler for the STOMP session
	 * @return a CompletableFuture for access to the session when ready for use
	 * @since 6.0
	 */
	public CompletableFuture<StompSession> connectAsync(StompSessionHandler handler) {
		return connectAsync(null, handler);
	}

	/**
	 * An overloaded version of {@link #connectAsync(StompSessionHandler)} that
	 * accepts headers to use for the STOMP CONNECT frame.
	 * @param connectHeaders headers to add to the CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return a CompletableFuture for access to the session when ready for use
	 */
	public CompletableFuture<StompSession> connectAsync(@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connectAsync(session);
		return session.getSession();
	}

	/**
	 * Shut down the client and release resources.
	 */
	public void shutdown() {
		this.tcpClient.shutdownAsync();
	}

	@Override
	public String toString() {
		return "ReactorNettyTcpStompClient[" + this.tcpClient + "]";
	}

}
