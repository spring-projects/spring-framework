/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link StompBrokerRelayMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
class StompBrokerRelayMessageHandlerTests {

	private StompBrokerRelayMessageHandler brokerRelay;

	private StubMessageChannel outboundChannel;

	private StubTcpOperations tcpClient;


	@BeforeEach
	void setup() {

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
	void virtualHost() {

		this.brokerRelay.setVirtualHost("ABC");

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertThat(this.tcpClient.getSentMessages().size()).isEqualTo(2);

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

		assertThat(this.tcpClient.getSentMessages().size()).isEqualTo(2);

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

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headers.setSessionId("sess1");
		headers.setDestination("/user/daisy/foo");
		this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()));

		assertThat(this.tcpClient.getSentMessages().size()).isEqualTo(1);
		StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
		assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
	}

	@Test
	void messageFromBrokerIsEnriched() {

		this.brokerRelay.start();
		this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

		assertThat(this.tcpClient.getSentMessages().size()).isEqualTo(2);
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

		MessageHandler handler = mock(MessageHandler.class);
		this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/foo", handler));
		this.brokerRelay.start();

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], headers));

		assertThat(this.tcpClient.getSentMessages().size()).isEqualTo(2);
		assertThat(this.tcpClient.getSentHeaders(0).getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
		assertThat(this.tcpClient.getSentHeaders(1).getDestination()).isEqualTo("/topic/foo");

		Message<byte[]> message = message(StompCommand.MESSAGE, null, null, "/topic/foo");
		this.tcpClient.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(handler).handleMessage(captor.capture());
		assertThat(captor.getValue()).isSameAs(message);
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
			public Void call() {
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
			assertThat(getSentMessages().size() > index).as("Size: " + getSentMessages().size()).isTrue();
			Message<byte[]> message = getSentMessages().get(index);
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			assertThat(accessor).isNotNull();
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
