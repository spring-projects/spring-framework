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
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.converter.MessageConverter;

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

	private MethodParameter sendToReturnType;

	private MethodParameter sendToUserReturnType;

	private MethodParameter missingSendToReturnType;


	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		Message message = MessageBuilder.withPayload(payloadContent).build();
		when(this.messageConverter.toMessage(payloadContent, null)).thenReturn(message);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(this.messageConverter);

		this.handler = new SendToMethodReturnValueHandler(messagingTemplate, true);
		this.handlerAnnotationNotRequired = new SendToMethodReturnValueHandler(messagingTemplate, false);

		Method method = this.getClass().getDeclaredMethod("handleAndSendTo");
		this.sendToReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUser");
		this.sendToUserReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleWithMissingSendTo");
		this.missingSendToReturnType = new MethodParameter(method, -1);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.sendToReturnType));
		assertTrue(this.handler.supportsReturnType(this.sendToUserReturnType));
		assertFalse(this.handler.supportsReturnType(this.missingSendToReturnType));
		assertTrue(this.handlerAnnotationNotRequired.supportsReturnType(this.missingSendToReturnType));
	}

	@Test
	public void sendToMethod() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/dest", null);

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
	public void sendToUserMethod() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/dest", user);

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


	private Message<?> createInputMessage(String sessId, String subsId, String dest, Principal principal) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setSessionId(sessId);
		headers.setSubscriptionId(subsId);
		headers.setDestination(dest);
		headers.setUser(principal);
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

	@MessageMapping("/handle")	// not needed for the tests but here for completeness
	public String handleWithMissingSendTo() {
		return payloadContent;
	}

	@MessageMapping("/handle")	// not needed for the tests but here for completeness
	@SendTo({"/dest1", "/dest2"})
	public String handleAndSendTo() {
		return payloadContent;
	}

	@MessageMapping("/handle")	// not needed for the tests but here for completeness
	@SendToUser({"/dest1", "/dest2"})
	public String handleAndSendToUser() {
		return payloadContent;
	}

}
