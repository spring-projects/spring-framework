/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A STOMP over TCP client that uses {@link ReactorNettyTcpClient}.
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
		this.tcpClient = new ReactorNettyTcpClient<>(host, port, new StompReactorNettyCodec());
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public ReactorNettyTcpStompClient(TcpOperations<byte[]> tcpClient) {
		Assert.notNull(tcpClient, "'tcpClient' is required");
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
	public ListenableFuture<StompSession> connect(@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {
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

}
