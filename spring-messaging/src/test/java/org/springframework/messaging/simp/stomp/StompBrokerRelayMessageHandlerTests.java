/*
 * Copyright 2002-2024 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StompBrokerRelayMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
class StompBrokerRelayMessageHandlerTests {

	private StompBrokerRelayMessageHandler brokerRelay;

	private final StubMessageChannel outboundChannel = new StubMessageChannel();

	private final StubTcpOperations tcpClient = new StubTcpOperations();

	private final ArgumentCaptor<Runnable> messageCountTaskCaptor = ArgumentCaptor.forClass(Runnable.class);


	@BeforeEach
	void setup() {
		this.brokerRelay = new StompBrokerRelayMessageHandler(new StubMessageChannel(),
				this.outboundChannel, new StubMessageChannel(), Collections.singletonList("/topic")) {

			@Override
			protected void startInternal() {
				publishBrokerAvailableEvent(); // Force this, since we'll never actually connect
				super.startInternal();
			}
		};

		this.brokerRelay.setTcpClient(this.tcpClient);

		this.brokerRelay.setTaskScheduler(mock());
	}

	@Test
	void virtualHost() {
		this.brokerRelay.setVirtualHost("ABC");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);

		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
		assertThat(headers1.getHost()).isEqualTo("ABC");

		StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
		assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers2.getSessionId()).isEqualTo("sess1");
		assertThat(headers2.getHost()).isEqualTo("ABC");
	}

	@Test
	void loginAndPasscode() {
		this.brokerRelay.setSystemLogin("syslogin");
		this.brokerRelay.setSystemPasscode("syspasscode");
		this.brokerRelay.setClientLogin("clientlogin");
		this.brokerRelay.setClientPasscode("clientpasscode");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);

		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers1.getLogin()).isEqualTo("syslogin");
		assertThat(headers1.getPasscode()).isEqualTo("syspasscode");

		StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
		assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers2.getLogin()).isEqualTo("clientlogin");
		assertThat(headers2.getPasscode()).isEqualTo("clientpasscode");
	}

	@Test
	void destinationExcluded() {
		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setLeaveMutable(true);
		this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setSessionId("sess1");
		accessor.setDestination("/user/daisy/foo");
		this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);
		StompHeaderAccessor headers = this.tcpClient.getSentHeaders(0);
		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);

		headers = this.tcpClient.getSentHeaders(1);
		assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers.getSessionId()).isEqualTo("sess1");
	}

	@Test // gh-22822
	void destinationExcludedWithHeartbeat() {
		Message<byte[]> connectMessage = connectMessage("sess1", "joe");
		MessageHeaderAccessor.getAccessor(connectMessage, StompHeaderAccessor.class).setHeartbeat(10000, 10000);

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage);

		SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setLeaveMutable(true);
		this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		// Run the messageCountTask to clear the message count
		verify(this.brokerRelay.getTaskScheduler()).scheduleWithFixedDelay(this.messageCountTaskCaptor.capture(), eq(Duration.ofMillis(5000L)));
		this.messageCountTaskCaptor.getValue().run();

		accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setSessionId("sess1");
		accessor.setDestination("/user/daisy/foo");
		this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		assertThat(this.tcpClient.getSentMessages()).hasSize(3);
		assertThat(this.tcpClient.getSentHeaders(2).getMessageType()).isEqualTo(SimpMessageType.HEARTBEAT);
	}

	@Test
	void messageFromBrokerIsEnriched() {
		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);
		assertThat(this.tcpClient.getSentHeaders(0).getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.CONNECT);

		this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, null));

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getUser().getName()).isEqualTo("joe");
	}

	// SPR-12820

	@Test
	void connectWhenBrokerNotAvailable() {
		this.brokerRelay.start();
		this.brokerRelay.stopInternal();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getUser().getName()).isEqualTo("joe");
		assertThat(accessor.getMessage()).isEqualTo("Broker not available.");
	}

	@Test
	void sendAfterBrokerUnavailable() {
		this.brokerRelay.start();
		assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);

		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
		assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(2);

		this.brokerRelay.stopInternal();
		this.brokerRelay.handleMessage(message(StompCommand.SEND, "sess1", "joe", "/foo"));
		assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);

		Message<byte[]> message = this.outboundChannel.getMessages().get(0);
		StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getUser().getName()).isEqualTo("joe");
		assertThat(accessor.getMessage()).isEqualTo("Broker not available.");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void systemSubscription() {
		MessageHandler handler = mock();
		this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/foo", handler));
		this.brokerRelay.start();

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], headers));

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);
		assertThat(this.tcpClient.getSentHeaders(0).getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(this.tcpClient.getSentHeaders(1).getDestination()).isEqualTo("/topic/foo");

		Message<byte[]> message = message(StompCommand.MESSAGE, null, null, "/topic/foo");
		this.tcpClient.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(handler).handleMessage(captor.capture());
		assertThat(captor.getValue()).isSameAs(message);
	}

	@Test
	void alreadyConnected() {
		this.brokerRelay.start();

		Message<byte[]> connect = connectMessage("sess1", "joe");
		this.brokerRelay.handleMessage(connect);

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);

		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);

		StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
		assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers2.getSessionId()).isEqualTo("sess1");

		this.brokerRelay.handleMessage(connect);

		assertThat(this.tcpClient.getSentMessages()).hasSize(2);
		assertThat(this.outboundChannel.getMessages()).isEmpty();
	}

	private Message<byte[]> connectMessage(String sessionId, String user) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setSessionId(sessionId);
		headers.setUser(new TestPrincipal(user));
		headers.setLeaveMutable(true);
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


	private static CompletableFuture<Void> getVoidFuture() {
		return CompletableFuture.completedFuture(null);
	}


	private static class StubTcpOperations implements TcpOperations<byte[]> {

		private StubTcpConnection connection = new StubTcpConnection();

		private TcpConnectionHandler<byte[]> connectionHandler;


		public List<Message<byte[]>> getSentMessages() {
			return this.connection.getMessages();
		}

		public StompHeaderAccessor getSentHeaders(int index) {
			assertThat(getSentMessages().size()).as("Size: " + getSentMessages().size()).isGreaterThan(index);
			Message<byte[]> message = getSentMessages().get(index);
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			assertThat(accessor).isNotNull();
			return accessor;
		}

		@Override
		public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler) {
			this.connectionHandler = handler;
			handler.afterConnected(this.connection);
			return getVoidFuture();
		}

		@Override
		public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler,
				ReconnectStrategy reconnectStrategy) {
			this.connectionHandler = handler;
			handler.afterConnected(this.connection);
			return getVoidFuture();
		}

		@Override
		public CompletableFuture<Void> shutdownAsync() {
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
		public CompletableFuture<Void> sendAsync(Message<byte[]> message) {
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
