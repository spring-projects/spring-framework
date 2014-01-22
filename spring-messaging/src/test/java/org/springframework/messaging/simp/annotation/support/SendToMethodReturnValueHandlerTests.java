/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.security.Principal;

import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.converter.MessageConverter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link SendToMethodReturnValueHandlerTests}.
 *
 * @author Rossen Stoyanchev
 */
public class SendToMethodReturnValueHandlerTests {

	private static final String payloadContent = "payload";


	private SendToMethodReturnValueHandler handler;

	private SendToMethodReturnValueHandler handlerAnnotationNotRequired;

	@Mock private MessageChannel messageChannel;

	@Captor ArgumentCaptor<Message<?>> messageCaptor;

	@Mock private MessageConverter messageConverter;

	private MethodParameter noAnnotationsReturnType;
	private MethodParameter sendToReturnType;
	private MethodParameter sendToDefaultDestReturnType;
	private MethodParameter sendToUserReturnType;
	private MethodParameter sendToUserDefaultDestReturnType;


	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		Message message = MessageBuilder.withPayload(payloadContent).build();
		when(this.messageConverter.toMessage(payloadContent, null)).thenReturn(message);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(this.messageConverter);

		this.handler = new SendToMethodReturnValueHandler(messagingTemplate, true);
		this.handlerAnnotationNotRequired = new SendToMethodReturnValueHandler(messagingTemplate, false);

		Method method = this.getClass().getDeclaredMethod("handleNoAnnotations");
		this.noAnnotationsReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToDefaultDestination");
		this.sendToDefaultDestReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendTo");
		this.sendToReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUser");
		this.sendToUserReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUserDefaultDestination");
		this.sendToUserDefaultDestReturnType = new MethodParameter(method, -1);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.sendToReturnType));
		assertTrue(this.handler.supportsReturnType(this.sendToUserReturnType));
		assertFalse(this.handler.supportsReturnType(this.noAnnotationsReturnType));
		assertTrue(this.handlerAnnotationNotRequired.supportsReturnType(this.noAnnotationsReturnType));
	}

	@Test
	public void sendToNoAnnotations() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		Message<?> inputMessage = createInputMessage("sess1", "sub1", "/dest", null);
		this.handler.handleReturnValue(payloadContent, this.noAnnotationsReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("sess1", headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/topic/dest", headers.getDestination());
	}

	@Test
	public void sendTo() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null);
		this.handler.handleReturnValue(payloadContent, this.sendToReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/dest1", headers.getDestination());

		message = this.messageCaptor.getAllValues().get(1);
		headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/dest2", headers.getDestination());
	}

	@Test
	public void sendToDefaultDestination() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/dest", null);
		this.handler.handleReturnValue(payloadContent, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/topic/dest", headers.getDestination());
	}

	@Test
	public void sendToUser() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, user);
		this.handler.handleReturnValue(payloadContent, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/dest1", headers.getDestination());

		message = this.messageCaptor.getAllValues().get(1);
		headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/dest2", headers.getDestination());
	}

	@Test
	public void sendToUserWithUserNameProvider() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		TestUser user = new UniqueUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, user);
		this.handler.handleReturnValue(payloadContent, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(this.messageCaptor.getAllValues().get(0));
		assertEquals("/user/Me myself and I/dest1", headers.getDestination());

		headers = SimpMessageHeaderAccessor.wrap(this.messageCaptor.getAllValues().get(1));
		assertEquals("/user/Me myself and I/dest2", headers.getDestination());
	}

	@Test
	public void sendToUserDefaultDestination() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/dest", user);
		this.handler.handleReturnValue(payloadContent, this.sendToUserDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertNull(headers.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/queue/dest", headers.getDestination());
	}


	private Message<?> createInputMessage(String sessId, String subsId, String destination, Principal principal) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setSessionId(sessId);
		headers.setSubscriptionId(subsId);
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (principal != null) {
			headers.setUser(principal);
		}
		return MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toMap()).build();
	}

	private static class TestUser implements Principal {

		public String getName() {
			return "joe";
		}

		public boolean implies(Subject subject) {
			return false;
		}
	}

	private static class UniqueUser extends TestUser implements DestinationUserNameProvider {

		@Override
		public String getDestinationUserName() {
			return "Me myself and I";
		}
	}

	public String handleNoAnnotations() {
		return payloadContent;
	}

	@SendTo
	public String handleAndSendToDefaultDestination() {
		return payloadContent;
	}

	@SendTo({"/dest1", "/dest2"})
	public String handleAndSendTo() {
		return payloadContent;
	}

	@SendToUser
	public String handleAndSendToUserDefaultDestination() {
		return payloadContent;
	}

	@SendToUser({"/dest1", "/dest2"})
	public String handleAndSendToUser() {
		return payloadContent;
	}

}
