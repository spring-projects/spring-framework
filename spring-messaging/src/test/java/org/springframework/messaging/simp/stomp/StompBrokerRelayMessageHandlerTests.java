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
package org.springframework.messaging.simp.stomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.tcp.ReconnectStrategy;
import org.springframework.messaging.support.tcp.TcpConnection;
import org.springframework.messaging.support.tcp.TcpConnectionHandler;
import org.springframework.messaging.support.tcp.TcpOperations;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import static org.junit.Assert.*;

/**
 * Unit tests for StompBrokerRelayMessageHandler.
 *
 * @author Rossen Stoyanchev
 */
public class StompBrokerRelayMessageHandlerTests {

	private StompBrokerRelayMessageHandler brokerRelay;

	private StubTcpOperations tcpClient;


	@Before
	public void setup() {

		this.tcpClient = new StubTcpOperations();

		this.brokerRelay = new StompBrokerRelayMessageHandler(new StubMessageChannel(), Arrays.asList("/topic"));
		this.brokerRelay.setTcpClient(tcpClient);
	}


	@Test
	public void testVirtualHostHeader() {

		String virtualHost = "ABC";
		String sessionId = "sess1";

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setSessionId(sessionId);

		this.brokerRelay.setVirtualHost(virtualHost);
		this.brokerRelay.start();
		this.brokerRelay.handleMessage(MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build());

		List<Message<byte[]>> sent = this.tcpClient.connection.messages;
		assertEquals(2, sent.size());

		StompHeaderAccessor headers1 = StompHeaderAccessor.wrap(sent.get(0));
		assertEquals(virtualHost, headers1.getHost());

		StompHeaderAccessor headers2 = StompHeaderAccessor.wrap(sent.get(1));
		assertEquals(sessionId, headers2.getSessionId());
		assertEquals(virtualHost, headers2.getHost());
	}

	@Test
	public void testDestinationExcluded() {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headers.setSessionId("sess1");
		headers.setDestination("/user/daisy/foo");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build());

		List<Message<byte[]>> sent = this.tcpClient.connection.messages;
		assertEquals(1, sent.size());
		assertEquals(StompCommand.CONNECT, StompHeaderAccessor.wrap(sent.get(0)).getCommand());
	}


	private static ListenableFutureTask<Void> getFuture() {
		ListenableFutureTask<Void> futureTask = new ListenableFutureTask<>(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				return null;
			}
		});
		futureTask.run();
		return futureTask;
	}


	private static class StubTcpOperations implements TcpOperations<byte[]> {

		private StubTcpConnection connection = new StubTcpConnection();


		@Override
		public ListenableFuture<Void> connect(TcpConnectionHandler<byte[]> connectionHandler) {
			connectionHandler.afterConnected(this.connection);
			return getFuture();
		}

		@Override
		public ListenableFuture<Void> connect(TcpConnectionHandler<byte[]> connectionHandler, ReconnectStrategy reconnectStrategy) {
			connectionHandler.afterConnected(this.connection);
			return getFuture();
		}

		@Override
		public ListenableFuture<Void> shutdown() {
			return getFuture();
		}
	}


	private static class StubTcpConnection implements TcpConnection<byte[]> {

		private final List<Message<byte[]>> messages = new ArrayList<>();


		@Override
		public ListenableFuture<Void> send(Message<byte[]> message) {
			this.messages.add(message);
			return getFuture();
		}

		@Override
		public void onReadInactivity(Runnable runnable, long duration) {
		}

		@Override
		public void onWriteInactivity(Runnable runnable, long duration) {
		}

		@Override
		public void close() {
		}
	}

}
