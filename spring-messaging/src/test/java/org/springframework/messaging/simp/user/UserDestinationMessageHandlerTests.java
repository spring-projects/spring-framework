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

package org.springframework.messaging.simp.user;

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
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.springframework.messaging.simp.user.UserDestinationMessageHandler}.
 */
public class UserDestinationMessageHandlerTests {

	private UserDestinationMessageHandler messageHandler;


	@Mock
	private SubscribableChannel brokerChannel;

	private UserSessionRegistry registry;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.registry = new DefaultUserSessionRegistry();
		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(this.registry);
		this.messageHandler = new UserDestinationMessageHandler(new StubMessageChannel(),
				new StubMessageChannel(), this.brokerChannel, resolver);
	}


	@Test
	public void handleSubscribe() {
		this.registry.registerSessionId("joe", "123");
		when(this.brokerChannel.send(Mockito.any(Message.class))).thenReturn(true);
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, "joe", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		assertEquals("/queue/foo-user123",
				captor.getValue().getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER));
	}

	@Test
	public void handleUnsubscribe() {
		this.registry.registerSessionId("joe", "123");
		when(this.brokerChannel.send(Mockito.any(Message.class))).thenReturn(true);
		this.messageHandler.handleMessage(createMessage(SimpMessageType.UNSUBSCRIBE, "joe", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		assertEquals("/queue/foo-user123",
				captor.getValue().getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER));
	}

	@Test
	public void handleMessage() {
		this.registry.registerSessionId("joe", "123");
		when(this.brokerChannel.send(Mockito.any(Message.class))).thenReturn(true);
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		assertEquals("/queue/foo-user123",
				captor.getValue().getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER));
	}


	@Test
	public void ignoreMessage() {

		// no destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", null));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// not a user destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// subscribe + no user
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, null, "/user/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// subscribe + not a user destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, "joe", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);

		// no match on message type
		this.messageHandler.handleMessage(createMessage(SimpMessageType.CONNECT, "joe", "user/joe/queue/foo"));
		Mockito.verifyZeroInteractions(this.brokerChannel);
	}


	private Message<?> createMessage(SimpMessageType messageType, String user, String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(messageType);
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (user != null) {
			headers.setUser(new TestPrincipal(user));
		}
		return MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
	}

}
