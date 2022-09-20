/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNetty2TcpClient;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A STOMP over TCP client, configurable with either
 * {@link ReactorNettyTcpClient} or {@link ReactorNetty2TcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyTcpStompClient extends StompClientSupport {

	private static final boolean reactorNettyClientPresent;

	private static final boolean reactorNetty2ClientPresent;

	static {
		ClassLoader classLoader = StompBrokerRelayMessageHandler.class.getClassLoader();
		reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", classLoader);
		reactorNetty2ClientPresent = ClassUtils.isPresent("reactor.netty5.http.client.HttpClient", classLoader);
	}


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
		if (reactorNettyClientPresent) {
			ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(host, port, new StompReactorNettyCodec());
			client.setLogger(SimpLogging.forLog(client.getLogger()));
			return client;
		}
		else if (reactorNetty2ClientPresent) {
			ReactorNetty2TcpClient<byte[]> client = new ReactorNetty2TcpClient<>(host, port, new StompTcpMessageCodec());
			client.setLogger(SimpLogging.forLog(client.getLogger()));
			return client;
		}
		throw new IllegalStateException("No compatible version of Reactor Netty");
	}


	/**
	 * Connect and notify the given {@link StompSessionHandler} when connected
	 * on the STOMP level.
	 * @param handler the handler for the STOMP session
	 * @return a ListenableFuture for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(StompSessionHandler)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			StompSessionHandler handler) {
		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(handler));
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
	 * An overloaded version of {@link #connect(StompSessionHandler)} that
	 * accepts headers to use for the STOMP CONNECT frame.
	 * @param connectHeaders headers to add to the CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return a ListenableFuture for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(StompHeaders, StompSessionHandler)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connectAsync(session);
		return session.getSessionFuture();
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
