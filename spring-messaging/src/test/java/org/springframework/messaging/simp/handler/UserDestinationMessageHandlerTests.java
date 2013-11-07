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

package org.springframework.messaging.simp.handler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link UserDestinationMessageHandler}.
 */
public class UserDestinationMessageHandlerTests {

	private UserDestinationMessageHandler messageHandler;

	private MessageSendingOperations<String> messagingTemplate;

	private UserSessionRegistry registry;


	@Before
	public void setup() {
		this.messagingTemplate = Mockito.mock(MessageSendingOperations.class);
		this.registry = new DefaultUserSessionRegistry();
		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(this.registry);
		this.messageHandler = new UserDestinationMessageHandler(this.messagingTemplate, resolver);
	}


	@Test
	public void handleSubscribe() {
		this.registry.registerSessionId("joe", "123");
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, "joe", "/user/queue/foo"));

		ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Message> captor2 = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.messagingTemplate).send(captor1.capture(), captor2.capture());

		assertEquals("/queue/foo-user123", captor1.getValue());
	}

	@Test
	public void handleUnsubscribe() {
		this.registry.registerSessionId("joe", "123");
		this.messageHandler.handleMessage(createMessage(SimpMessageType.UNSUBSCRIBE, "joe", "/user/queue/foo"));

		ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Message> captor2 = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.messagingTemplate).send(captor1.capture(), captor2.capture());

		assertEquals("/queue/foo-user123", captor1.getValue());
	}

	@Test
	public void handleMessage() {
		this.registry.registerSessionId("joe", "123");
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", "/user/joe/queue/foo"));

		ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Message> captor2 = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.messagingTemplate).send(captor1.capture(), captor2.capture());

		assertEquals("/queue/foo-user123", captor1.getValue());
	}


	@Test
	public void ignoreMessage() {

		// no destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", null));
		Mockito.verifyZeroInteractions(this.messagingTemplate);

		// not a user destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.MESSAGE, "joe", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.messagingTemplate);

		// subscribe + no user
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, null, "/user/queue/foo"));
		Mockito.verifyZeroInteractions(this.messagingTemplate);

		// subscribe + not a user destination
		this.messageHandler.handleMessage(createMessage(SimpMessageType.SUBSCRIBE, "joe", "/queue/foo"));
		Mockito.verifyZeroInteractions(this.messagingTemplate);

		// no match on message type
		this.messageHandler.handleMessage(createMessage(SimpMessageType.CONNECT, "joe", "user/joe/queue/foo"));
		Mockito.verifyZeroInteractions(this.messagingTemplate);
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
