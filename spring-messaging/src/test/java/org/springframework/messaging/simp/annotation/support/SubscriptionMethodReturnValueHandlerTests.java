/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link SubscriptionMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class SubscriptionMethodReturnValueHandlerTests {

	public static final MimeType MIME_TYPE = new MimeType("text", "plain", Charset.forName("UTF-8"));

	private static final String PAYLOAD = "payload";


	private SubscriptionMethodReturnValueHandler handler;

	@Mock private MessageChannel messageChannel;

	@Captor ArgumentCaptor<Message<?>> messageCaptor;

	private MethodParameter subscribeEventReturnType;

	private MethodParameter subscribeEventSendToReturnType;

	private MethodParameter messageMappingReturnType;


	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());

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
	public void testMessageSentToChannel() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		String subscriptionId = "subs1";
		String destination = "/dest";
		Message<?> inputMessage = createInputMessage(sessionId, subscriptionId, destination, null);

		this.handler.handleReturnValue(PAYLOAD, this.subscribeEventReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		assertNotNull(this.messageCaptor.getValue());

		Message<?> message = this.messageCaptor.getValue();
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);

		assertNull("SimpMessageHeaderAccessor should have disabled id", headerAccessor.getId());
		assertNull("SimpMessageHeaderAccessor should have disabled timestamp", headerAccessor.getTimestamp());
		assertEquals(sessionId, headerAccessor.getSessionId());
		assertEquals(subscriptionId, headerAccessor.getSubscriptionId());
		assertEquals(destination, headerAccessor.getDestination());
		assertEquals(MIME_TYPE, headerAccessor.getContentType());
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testHeadersPassedToMessagingTemplate() throws Exception {

		String sessionId = "sess1";
		String subscriptionId = "subs1";
		String destination = "/dest";
		Message<?> inputMessage = createInputMessage(sessionId, subscriptionId, destination, null);

		MessageSendingOperations messagingTemplate = Mockito.mock(MessageSendingOperations.class);
		SubscriptionMethodReturnValueHandler handler = new SubscriptionMethodReturnValueHandler(messagingTemplate);

		handler.handleReturnValue(PAYLOAD, this.subscribeEventReturnType, inputMessage);

		ArgumentCaptor<MessageHeaders> captor = ArgumentCaptor.forClass(MessageHeaders.class);
		verify(messagingTemplate).convertAndSend(eq("/dest"), eq(PAYLOAD), captor.capture());

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(captor.getValue(), SimpMessageHeaderAccessor.class);

		assertNotNull(headerAccessor);
		assertTrue(headerAccessor.isMutable());
		assertEquals(sessionId, headerAccessor.getSessionId());
		assertEquals(subscriptionId, headerAccessor.getSubscriptionId());
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
		return PAYLOAD;
	}

	@SubscribeMapping("/data") // not needed for the tests but here for completeness
	@SendTo("/sendToDest")
	private String getDataAndSendTo() {
		return PAYLOAD;
	}

	@MessageMapping("/handle")	// not needed for the tests but here for completeness
	public String handle() {
		return PAYLOAD;
	}
}
