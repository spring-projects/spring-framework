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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * Unit tests for StompBrokerRelayMessageHandler.
 *
 * @author Rossen Stoyanchev
 */
public class StompBrokerRelayMessageHandlerTests {

	private StompBrokerRelayMessageHandler brokerRelay;

	private StubMessageChannel outboundChannel;

	private StubTcpOperations tcpClient;


	@Before
	public void setup() {

		this.outboundChannel = new StubMessageChannel();

		this.brokerRelay = new StompBrokerRelayMessageHandler(new StubMessageChannel(),
				this.outboundChannel, new StubMessageChannel(), Arrays.asList("/topic")) {

			@Override
			protected void startInternal() {
				publishBrokerAvailableEvent(); // Force this, since we'll never actually connect
				super.startInternal();
			}
		};

		this.tcpClient = new StubTcpOperations();
		this.brokerRelay.setTcpClient(this.tcpClient);
	}


	@Test
	public void virtualHost() throws Exception {

		this.brokerRelay.setVirtualHost("ABC");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertEquals(2, this.tcpClient.getSentMessages().size());

		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertEquals(StompCommand.CONNECT, headers1.getCommand());
		assertEquals(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID, headers1.getSessionId());
		assertEquals("ABC", headers1.getHost());

		StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
		assertEquals(StompCommand.CONNECT, headers2.getCommand());
		assertEquals("sess1", headers2.getSessionId());
		assertEquals("ABC", headers2.getHost());
	}

	@Test
	public void loginAndPasscode() throws Exception {

		this.brokerRelay.setSystemLogin("syslogin");
		this.brokerRelay.setSystemPasscode("syspasscode");
		this.brokerRelay.setClientLogin("clientlogin");
		this.brokerRelay.setClientPasscode("clientpasscode");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertEquals(2, this.tcpClient.getSentMessages().size());

		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertEquals(StompCommand.CONNECT, headers1.getCommand());
		assertEquals("syslogin", headers1.getLogin());
		assertEquals("syspasscode", headers1.getPasscode());

		StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
		assertEquals(StompCommand.CONNECT, headers2.getCommand());
		assertEquals("clientlogin", headers2.getLogin());
		assertEquals("clientpasscode", headers2.getPasscode());
	}

	@Test
	public void destinationExcluded() throws Exception {

		this.brokerRelay.start();

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headers.setSessionId("sess1");
		headers.setDestination("/user/daisy/foo");
		this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()));

		assertEquals(1, this.tcpClient.getSentMessages().size());
		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertEquals(StompCommand.CONNECT, headers1.getCommand());
		assertEquals(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID, headers1.getSessionId());
	}

	@Test
	public void messageFromBrokerIsEnriched() throws Exception {

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertEquals(2, this.tcpClient.getSentMessages().size());
		assertEquals(StompCommand.CONNECT, this.tcpClient.getSentHeaders(0).getCommand());
		assertEquals(StompCommand.CONNECT, this.tcpClient.getSentHeaders(1).getCommand());

		this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, null));

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertEquals("sess1", accessor.getSessionId());
		assertEquals("joe", accessor.getUser().getName());
	}

	// SPR-12820

	@Test
	public void connectWhenBrokerNotAvailable() throws Exception {

		this.brokerRelay.start();
		this.brokerRelay.stopInternal();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertEquals(StompCommand.ERROR, accessor.getCommand());
		assertEquals("sess1", accessor.getSessionId());
		assertEquals("joe", accessor.getUser().getName());
		assertEquals("Broker not available.", accessor.getMessage());
	}

	@Test
	public void sendAfterBrokerUnavailable() throws Exception {

		this.brokerRelay.start();
		assertEquals(1, this.brokerRelay.getConnectionCount());

		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
		assertEquals(2, this.brokerRelay.getConnectionCount());

		this.brokerRelay.stopInternal();
		this.brokerRelay.handleMessage(message(StompCommand.SEND, "sess1", "joe", "/foo"));
		assertEquals(1, this.brokerRelay.getConnectionCount());

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertEquals(StompCommand.ERROR, accessor.getCommand());
		assertEquals("sess1", accessor.getSessionId());
		assertEquals("joe", accessor.getUser().getName());
		assertEquals("Broker not available.", accessor.getMessage());
	}

	@Test
	public void systemSubscription() throws Exception {

		MessageHandler handler = mock(MessageHandler.class);
		this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/foo", handler));
		this.brokerRelay.start();

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], headers));

		assertEquals(2, this.tcpClient.getSentMessages().size());
		assertEquals(StompCommand.CONNECT, this.tcpClient.getSentHeaders(0).getCommand());
		assertEquals(StompCommand.SUBSCRIBE, this.tcpClient.getSentHeaders(1).getCommand());
		assertEquals("/topic/foo", this.tcpClient.getSentHeaders(1).getDestination());

		Message<byte[]> message = message(StompCommand.MESSAGE, null, null, "/topic/foo");
		this.tcpClient.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(handler).handleMessage(captor.capture());
		assertSame(message, captor.getValue());
	}

	private Message<byte[]> connectMessage(String sessionId, String user) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setSessionId(sessionId);
		headers.setUser(new TestPrincipal(user));
		return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
	}

	private Message<byte[]> message(StompCommand command, String sessionId, String user, String destination) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		if (sessionId != null) {
			accessor.setSessionId(sessionId);
		}
		if (user != null) {
			accessor.setUser(new TestPrincipal(user));
		}
		if (destination != null) {
			accessor.setDestination(destination);
		}
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}


	private static ListenableFutureTask<Void> getVoidFuture() {
		ListenableFutureTask<Void> futureTask = new ListenableFutureTask<>(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				return null;
			}
		});
		futureTask.run();
		return futureTask;
	}

	private static ListenableFutureTask<Boolean> getBooleanFuture() {
		ListenableFutureTask<Boolean> futureTask = new ListenableFutureTask<>(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return null;
			}
		});
		futureTask.run();
		return futureTask;
	}


	private static class StubTcpOperations implements TcpOperations<byte[]> {

		private StubTcpConnection connection = new StubTcpConnection();

		private TcpConnectionHandler<byte[]> connectionHandler;


		public List<Message<byte[]>> getSentMessages() {
			return this.connection.getMessages();
		}

		public StompHeaderAccessor getSentHeaders(int index) {
			assertTrue("Size: " + getSentMessages().size(), getSentMessages().size() > index);
			Message<byte[]> message = getSentMessages().get(index);
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			assertNotNull(accessor);
			return accessor;
		}

		@Override
		public ListenableFuture<Void> connect(TcpConnectionHandler<byte[]> handler) {
			this.connectionHandler = handler;
			handler.afterConnected(this.connection);
			return getVoidFuture();
		}

		@Override
		public ListenableFuture<Void> connect(TcpConnectionHandler<byte[]> handler, ReconnectStrategy strategy) {
			this.connectionHandler = handler;
			handler.afterConnected(this.connection);
			return getVoidFuture();
		}

		@Override
		public ListenableFuture<Void> shutdown() {
			return getVoidFuture();
		}

		public void handleMessage(Message<byte[]> message) {
			this.connectionHandler.handleMessage(message);
		}

	}


	private static class StubTcpConnection implements TcpConnection<byte[]> {

		private final List<Message<byte[]>> messages = new ArrayList<>();


		public List<Message<byte[]>> getMessages() {
			return this.messages;
		}

		@Override
		public ListenableFuture<Void> send(Message<byte[]> message) {
			this.messages.add(message);
			return getVoidFuture();
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
