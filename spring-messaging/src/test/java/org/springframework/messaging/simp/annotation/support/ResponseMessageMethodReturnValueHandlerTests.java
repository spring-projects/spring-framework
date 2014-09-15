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
import java.nio.charset.Charset;
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
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.ResponseMessage;
import org.springframework.util.MimeType;

import static org.junit.Assert.*;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


/**
 * Test fixture for {@link ResponseMessageMethodReturnValueHandler}
 * 
 * @author Sergi Almar
 */
public class ResponseMessageMethodReturnValueHandlerTests {

	public static final MimeType MIME_TYPE = new MimeType("text", "plain", Charset.forName("UTF-8"));
	
	private static final String PAYLOAD = "payload";
	
	private static final String USERNAME = "sergi";

	
	private ResponseMessageMethodReturnValueHandler handler;
	
	@Mock private MessageChannel messageChannel;
	
	@Captor ArgumentCaptor<Message<?>> messageCaptor;
	
	private MethodParameter responseMessageDestinationReturnType;
	private MethodParameter responseMessageDefaultDestinationReturnType;
	private MethodParameter responseMessageUserDestinationReturnType;
	private MethodParameter responseMessageUserDestinationMultipleReturnType;
	private MethodParameter responseMessageCurrentUserDestinationReturnType;
	private MethodParameter responseMessageCurrentUserSingleSessionDestinationReturnType;
	
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());

		this.handler = new ResponseMessageMethodReturnValueHandler(messagingTemplate);
		
		Method method = this.getClass().getDeclaredMethod("handleAndSendToDestination");
		this.responseMessageDestinationReturnType = new MethodParameter(method, -1);
		
		method = this.getClass().getDeclaredMethod("handleAndSendToDefaultDestination");
		this.responseMessageDefaultDestinationReturnType = new MethodParameter(method, -1);
		
		method = this.getClass().getDeclaredMethod("handleAndSendToUser");
		this.responseMessageUserDestinationReturnType = new MethodParameter(method, -1);
		
		method = this.getClass().getDeclaredMethod("handleAndSendToUserMultiple");
		this.responseMessageUserDestinationMultipleReturnType = new MethodParameter(method, -1);
		
		method = this.getClass().getDeclaredMethod("handleAndSendToCurrentUser");
		this.responseMessageCurrentUserDestinationReturnType = new MethodParameter(method, -1);
		
		method = this.getClass().getDeclaredMethod("handleAndSendToCurrentUser");
		this.responseMessageCurrentUserSingleSessionDestinationReturnType = new MethodParameter(method, -1);
	}
	
	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.responseMessageDestinationReturnType));
	}

	@Test
	public void testResponseMessageDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		ResponseMessage<?> responseMessage = new ResponseMessage<String>(PAYLOAD, "/topic/dest1");
		this.handler.handleReturnValue(responseMessage, this.responseMessageDestinationReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals(sessionId, headers.getSessionId());
		assertEquals("/topic/dest1", headers.getDestination());
		assertEquals(MIME_TYPE, headers.getContentType());
		assertNull("Subscription id should not be copied", headers.getSubscriptionId());
	}
	
	@Test
	public void testResponseMessageDefaultDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		ResponseMessage<?> responseMessage = new ResponseMessage<String>(PAYLOAD);
		this.handler.handleReturnValue(responseMessage, this.responseMessageDefaultDestinationReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/topic/dest", headers.getDestination());
	}
	
	@Test
	public void testResponseMessageUserDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		ResponseMessage<?> responseMessage = ResponseMessage.destination("/queue/dest1").toUser(USERNAME).body(PAYLOAD);
		this.handler.handleReturnValue(responseMessage, this.responseMessageUserDestinationReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/" + USERNAME + "/queue/dest1", headers.getDestination());
		assertEquals(sessionId, headers.getSessionId());
	}
	
	@Test
	public void testResponseMessageUserDestinationMultiple() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		ResponseMessage<?> responseMessage = ResponseMessage.destinations("/queue/dest1", "/queue/dest2").toUser(USERNAME).body(PAYLOAD);
		this.handler.handleReturnValue(responseMessage, this.responseMessageUserDestinationMultipleReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/" + USERNAME + "/queue/dest1", headers.getDestination());
		assertEquals(sessionId, headers.getSessionId());
		
		message = this.messageCaptor.getAllValues().get(1);
		headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/" + USERNAME + "/queue/dest2", headers.getDestination());
		assertEquals(sessionId, headers.getSessionId());
	}
	
	@Test
	public void testResponseMessageCurrentUserDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", user);
		ResponseMessage<?> responseMessage = ResponseMessage.destination("/queue/dest1").toCurrentUser().body(PAYLOAD);
		this.handler.handleReturnValue(responseMessage, this.responseMessageCurrentUserDestinationReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/" + user.getName() + "/queue/dest1", headers.getDestination());
		assertNull(headers.getSessionId());
		assertNull(headers.getSubscriptionId());
	}
	
	@Test
	public void testResponseMessageCurrentUserSingleSessionDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", user);
		ResponseMessage<?> responseMessage = ResponseMessage.destination("/queue/dest1").toCurrentUserNoBroadcast().body(PAYLOAD);
		this.handler.handleReturnValue(responseMessage, this.responseMessageCurrentUserSingleSessionDestinationReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		Message<?> message = this.messageCaptor.getAllValues().get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/" + user.getName() + "/queue/dest1", headers.getDestination());
		assertEquals(sessionId, headers.getSessionId());
		assertEquals(MIME_TYPE, headers.getContentType());
		assertNull("Subscription id should not be copied", headers.getSubscriptionId());
	}
	
	private Message<?> createInputMessage(String sessId, String subsId, String destinationPrefix,
            String destination, Principal principal) {

		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
		headerAccessor.setSessionId(sessId);
		headerAccessor.setSubscriptionId(subsId);
		if (destination != null && destinationPrefix != null) {
			headerAccessor.setDestination(destinationPrefix + destination);
			headerAccessor.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination);
		}
		if (principal != null) {
			headerAccessor.setUser(principal);
		}
		return MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
	}
	
	private static class TestUser implements Principal {

		public String getName() {
			return "joe";
		}

		public boolean implies(Subject subject) {
			return false;
		}
	}
	
	public ResponseMessage<String> handleAndSendToDestination() {
		return new ResponseMessage<String>(PAYLOAD, "/topic/dest1");
	}
	
	public ResponseMessage<String> handleAndSendToDefaultDestination() {
		return new ResponseMessage<String>(PAYLOAD);
	}
	
	public ResponseMessage<String> handleAndSendToUser() {
		return ResponseMessage.destination("/queue/dest1").toUser(USERNAME).body(PAYLOAD);
	}
	
	public ResponseMessage<String> handleAndSendToUserMultiple() {
		return ResponseMessage.destinations("/queue/dest1", "/queue/dest2").toUser(USERNAME).body(PAYLOAD);
	}
	
	public ResponseMessage<String> handleAndSendToCurrentUser() {
		return ResponseMessage.destination("/queue/dest1").toCurrentUser().body(PAYLOAD);
	}
	
	public ResponseMessage<String> handleAndSendToCurrentUserSingleSession() {
		return ResponseMessage.destination("/queue/dest1").toCurrentUserNoBroadcast().body(PAYLOAD);
	}
}
