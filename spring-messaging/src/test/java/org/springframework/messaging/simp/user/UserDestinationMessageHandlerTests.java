/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.user;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit tests for
 * {@link org.springframework.messaging.simp.user.UserDestinationMessageHandler}.
 */
public class UserDestinationMessageHandlerTests {

	private static final String SESSION_ID = "123";

	private UserDestinationMessageHandler handler;

	private SimpUserRegistry registry;

	private SubscribableChannel brokerChannel;


	@Before
	public void setup() {
		this.registry = mock(SimpUserRegistry.class);
		this.brokerChannel = mock(SubscribableChannel.class);
		UserDestinationResolver resolver = new DefaultUserDestinationResolver(this.registry);
		this.handler = new UserDestinationMessageHandler(new StubMessageChannel(), this.brokerChannel, resolver);
	}


	@Test
	public void handleSubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertEquals("/queue/foo-user123", SimpMessageHeaderAccessor.getDestination(message.getHeaders()));
	}

	@Test
	public void handleUnsubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.UNSUBSCRIBE, "joe", "123", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertEquals("/queue/foo-user123", SimpMessageHeaderAccessor.getDestination(message.getHeaders()));
	}

	@Test
	public void handleMessage() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		when(this.registry.getUser("joe")).thenReturn(simpUser);
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertEquals("/queue/foo-user123", accessor.getDestination());
		assertEquals("/user/queue/foo", accessor.getFirstNativeHeader(ORIGINAL_DESTINATION));
	}

	@Test
	public void handleMessageWithoutActiveSession() {
		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/topic/unresolved", accessor.getDestination());
		assertEquals("/user/joe/queue/foo", accessor.getFirstNativeHeader(ORIGINAL_DESTINATION));

		// Should ignore our own broadcast to brokerChannel

		this.handler.handleMessage(message);
		Mockito.verifyNoMoreInteractions(this.brokerChannel);
	}

	@Test
	public void handleMessageFromBrokerWithActiveSession() {

		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		when(this.registry.getUser("joe")).thenReturn(simpUser);

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
		assertNotNull(captor.getValue());
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertEquals("/queue/foo-user123", headers.getDestination());
		assertEquals("/user/queue/foo", headers.getFirstNativeHeader(ORIGINAL_DESTINATION));
		assertEquals("customHeaderValue", headers.getFirstNativeHeader("customHeader"));
		assertArrayEquals(payload, (byte[]) captor.getValue().getPayload());
	}

	@Test
	public void handleMessageFromBrokerWithoutActiveSession() {
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
	public void ignoreMessage() {

		// no destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", null));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// subscribe + not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", "123", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// no match on message type
		this.handler.handleMessage(createWith(SimpMessageType.CONNECT, "joe", "123", "user/joe/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);
	}


	private Message<?> createWith(SimpMessageType type, String user, String sessionId, String destination) {
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
