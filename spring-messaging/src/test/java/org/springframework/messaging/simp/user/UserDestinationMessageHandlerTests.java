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

package org.springframework.messaging.simp.user;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.ORIGINAL_DESTINATION;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit tests for
 * {@link org.springframework.messaging.simp.user.UserDestinationMessageHandler}.
 */
public class UserDestinationMessageHandlerTests {

	private static final String SESSION_ID = "123";


	private UserDestinationMessageHandler handler;

	private UserSessionRegistry registry;

	@Mock
	private SubscribableChannel brokerChannel;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.registry = new DefaultUserSessionRegistry();
		UserDestinationResolver resolver = new DefaultUserDestinationResolver(this.registry);
		this.handler = new UserDestinationMessageHandler(new StubMessageChannel(), this.brokerChannel, resolver);
	}


	@Test
	@SuppressWarnings("rawtypes")
	public void handleSubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertEquals("/queue/foo-user123", SimpMessageHeaderAccessor.getDestination(message.getHeaders()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void handleUnsubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.UNSUBSCRIBE, "joe", "123", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertEquals("/queue/foo-user123", SimpMessageHeaderAccessor.getDestination(message.getHeaders()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void handleMessage() {
		this.registry.registerSessionId("joe", "123");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertEquals("/queue/foo-user123", accessor.getDestination());
		assertEquals("/user/queue/foo", accessor.getFirstNativeHeader(ORIGINAL_DESTINATION));
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
