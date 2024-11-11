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

package org.springframework.messaging.simp.user;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.ORIGINAL_DESTINATION;

/**
 * Tests for {@link UserDestinationMessageHandler}.
 */
class UserDestinationMessageHandlerTests {

	private static final String SESSION_ID = "123";

	private final SimpUserRegistry registry = mock();

	private final SubscribableChannel brokerChannel = mock();

	private final UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
			new StubMessageChannel(), this.brokerChannel, new DefaultUserDestinationResolver(this.registry));


	@Test
	@SuppressWarnings("rawtypes")
	void handleSubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleUnsubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.UNSUBSCRIBE, "joe", "123", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessage() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		given(this.registry.getUser("joe")).willReturn(simpUser);
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertThat(accessor.getDestination()).isEqualTo("/queue/foo-user123");
		assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessageWithoutActiveSession() {
		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
		assertThat(accessor.getDestination()).isEqualTo("/topic/unresolved");
		assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/joe/queue/foo");

		// Should ignore our own broadcast to brokerChannel

		this.handler.handleMessage(message);
		Mockito.verifyNoMoreInteractions(this.brokerChannel);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessageFromBrokerWithActiveSession() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		given(this.registry.getUser("joe")).willReturn(simpUser);

		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setSessionId("system123");
		accessor.setDestination("/topic/unresolved");
		accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
		accessor.setNativeHeader("customHeader", "customHeaderValue");
		accessor.setLeaveMutable(true);
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
		this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());
		assertThat(captor.getValue()).isNotNull();
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertThat(headers.getDestination()).isEqualTo("/queue/foo-user123");
		assertThat(headers.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
		assertThat(headers.getFirstNativeHeader("customHeader")).isEqualTo("customHeaderValue");
		assertThat((byte[]) captor.getValue().getPayload()).isEqualTo(payload);
	}

	@Test
	void handleMessageFromBrokerWithoutActiveSession() {
		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setSessionId("system123");
		accessor.setDestination("/topic/unresolved");
		accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
		accessor.setLeaveMutable(true);
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
		this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		// No re-broadcast
		verifyNoMoreInteractions(this.brokerChannel);
	}

	@Test
	void ignoreMessage() {

		// no destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", null));
		verifyNoInteractions(this.brokerChannel);

		// not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/queue/foo"));
		verifyNoInteractions(this.brokerChannel);

		// subscribe + not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", "123", "/queue/foo"));
		verifyNoInteractions(this.brokerChannel);

		// no match on message type
		this.handler.handleMessage(createWith(SimpMessageType.CONNECT, "joe", "123", "user/joe/queue/foo"));
		verifyNoInteractions(this.brokerChannel);
	}


	private Message<?> createWith(
			SimpMessageType type, @Nullable String user, @Nullable String sessionId, @Nullable String destination) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (user != null) {
			headers.setUser(new TestPrincipal(user));
		}
		if (sessionId != null) {
			headers.setSessionId(sessionId);
		}
		return MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
	}

}
