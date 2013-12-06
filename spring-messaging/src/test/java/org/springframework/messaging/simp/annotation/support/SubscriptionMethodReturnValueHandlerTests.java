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
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.converter.MessageConverter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link SubscriptionMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class SubscriptionMethodReturnValueHandlerTests {

	private static final String payloadContent = "payload";


	private SubscriptionMethodReturnValueHandler handler;

	@Mock private MessageChannel messageChannel;

	@Captor ArgumentCaptor<Message<?>> messageCaptor;

	@Mock private MessageConverter messageConverter;

	private MethodParameter subscribeEventReturnType;

	private MethodParameter subscribeEventSendToReturnType;

	private MethodParameter messageMappingReturnType;


	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		Message message = MessageBuilder.withPayload(payloadContent).build();
		when(this.messageConverter.toMessage(payloadContent, null)).thenReturn(message);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(this.messageConverter);

		this.handler = new SubscriptionMethodReturnValueHandler(messagingTemplate);

		Method method = this.getClass().getDeclaredMethod("getData");
		this.subscribeEventReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("getDataAndSendTo");
		this.subscribeEventSendToReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handle");
		this.messageMappingReturnType = new MethodParameter(method, -1);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.subscribeEventReturnType));
		assertFalse(this.handler.supportsReturnType(this.subscribeEventSendToReturnType));
		assertFalse(this.handler.supportsReturnType(this.messageMappingReturnType));
	}

	@Test
	public void subscribeEventMethod() throws Exception {

		when(this.messageChannel.send(any(Message.class))).thenReturn(true);

		String sessionId = "sess1";
		String subscriptionId = "subs1";
		String destination = "/dest";
		Message<?> inputMessage = createInputMessage(sessionId, subscriptionId, destination, null);

		this.handler.handleReturnValue(payloadContent, this.subscribeEventReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		assertNotNull(this.messageCaptor.getValue());

		Message<?> message = this.messageCaptor.getValue();
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);

		assertEquals("sessionId should always be copied", sessionId, headers.getSessionId());
		assertEquals(subscriptionId, headers.getSubscriptionId());
		assertEquals(destination, headers.getDestination());
	}


	private Message<?> createInputMessage(String sessId, String subsId, String dest, Principal principal) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setSessionId(sessId);
		headers.setSubscriptionId(subsId);
		headers.setDestination(dest);
		headers.setUser(principal);
		return MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toMap()).build();
	}


	@SubscribeMapping("/data") // not needed for the tests but here for completeness
	private String getData() {
		return payloadContent;
	}

	@SubscribeMapping("/data") // not needed for the tests but here for completeness
	@SendTo("/sendToDest")
	private String getDataAndSendTo() {
		return payloadContent;
	}

	@MessageMapping("/handle")	// not needed for the tests but here for completeness
	public String handle() {
		return payloadContent;
	}
}
