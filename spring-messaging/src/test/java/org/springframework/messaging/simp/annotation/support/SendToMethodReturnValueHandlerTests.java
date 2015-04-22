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

package org.springframework.messaging.simp.annotation.support;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER;
import static org.springframework.messaging.support.MessageHeaderAccessor.*;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.security.auth.Subject;

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
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;

/**
 * Test fixture for {@link SendToMethodReturnValueHandlerTests}.
 *
 * @author Rossen Stoyanchev
 */
public class SendToMethodReturnValueHandlerTests {

	private static final MimeType MIME_TYPE = new MimeType("text", "plain", Charset.forName("UTF-8"));

	private static final String PAYLOAD = "payload";


	private SendToMethodReturnValueHandler handler;

	private SendToMethodReturnValueHandler handlerAnnotationNotRequired;

	@Mock private MessageChannel messageChannel;

	@Captor ArgumentCaptor<Message<?>> messageCaptor;

	private MethodParameter noAnnotationsReturnType;
	private MethodParameter sendToReturnType;
	private MethodParameter sendToDefaultDestReturnType;
	private MethodParameter sendToWithPlaceholdersType;
	private MethodParameter sendToUserReturnType;
	private MethodParameter sendToUserSingleSessionReturnType;
	private MethodParameter sendToUserDefaultDestReturnType;
	private MethodParameter sendToUserSingleSessionDefaultDestReturnType;


	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());

		this.handler = new SendToMethodReturnValueHandler(messagingTemplate, true);
		this.handlerAnnotationNotRequired = new SendToMethodReturnValueHandler(messagingTemplate, false);

		Method method = this.getClass().getDeclaredMethod("handleNoAnnotations");
		this.noAnnotationsReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToDefaultDestination");
		this.sendToDefaultDestReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendTo");
		this.sendToReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToWithPlaceholders");
		this.sendToWithPlaceholdersType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUser");
		this.sendToUserReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUserSingleSession");
		this.sendToUserSingleSessionReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUserDefaultDestination");
		this.sendToUserDefaultDestReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handleAndSendToUserDefaultDestinationSingleSession");
		this.sendToUserSingleSessionDefaultDestReturnType = new MethodParameter(method, -1);
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

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Message<?> inputMessage = createInputMessage("sess1", "sub1", "/app", "/dest", null);
		this.handler.handleReturnValue(PAYLOAD, this.noAnnotationsReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("sess1", accessor.getSessionId());
		assertEquals("/topic/dest", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendTo() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/dest1", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());

		accessor = getCapturedAccessor(1);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/dest2", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendToDefaultDestination() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/topic/dest", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendToDefaultDestinationWhenUsingDotPathSeparator() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Message<?> inputMessage = createInputMessage("sess1", "sub1", "/app/", "dest.foo.bar", null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("/topic/dest.foo.bar", accessor.getDestination());
	}

	@Test
	public void testHeadersToSend() throws Exception {

		Message<?> inputMessage = createInputMessage("sess1", "sub1", "/app", "/dest", null);

		SimpMessageSendingOperations messagingTemplate = Mockito.mock(SimpMessageSendingOperations.class);
		SendToMethodReturnValueHandler handler = new SendToMethodReturnValueHandler(messagingTemplate, false);

		handler.handleReturnValue(PAYLOAD, this.noAnnotationsReturnType, inputMessage);

		ArgumentCaptor<MessageHeaders> captor = ArgumentCaptor.forClass(MessageHeaders.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/dest"), eq(PAYLOAD), captor.capture());

		MessageHeaders messageHeaders = captor.getValue();
		SimpMessageHeaderAccessor accessor = getAccessor(messageHeaders, SimpMessageHeaderAccessor.class);
		assertNotNull(accessor);
		assertTrue(accessor.isMutable());
		assertEquals("sess1", accessor.getSessionId());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendToUser() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertNull(accessor.getSessionId());
		assertNull(accessor.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/dest1", accessor.getDestination());

		accessor = getCapturedAccessor(1);
		assertNull(accessor.getSessionId());
		assertNull(accessor.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/dest2", accessor.getDestination());
	}

	// SPR-12170

	@Test
	public void sendToWithDestinationPlaceholders() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Map<String, String> vars = new LinkedHashMap<>(1);
		vars.put("roomName", "roomA");

		String sessionId = "sess1";
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId("sub1");
		accessor.setHeader(DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
		Message<?> message = MessageBuilder.createMessage(PAYLOAD, accessor.getMessageHeaders());
		this.handler.handleReturnValue(PAYLOAD, this.sendToWithPlaceholdersType, message);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor actual = getCapturedAccessor(0);
		assertEquals(sessionId, actual.getSessionId());
		assertEquals("/topic/chat.message.filtered.roomA", actual.getDestination());
	}

	@Test
	public void sendToUserSingleSession() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserSingleSessionReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertEquals("/user/" + user.getName() + "/dest1", accessor.getDestination());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());

		accessor = getCapturedAccessor(1);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/user/" + user.getName() + "/dest2", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendToUserWithUserNameProvider() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new UniqueUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("/user/Me myself and I/dest1", accessor.getDestination());

		accessor = getCapturedAccessor(1);
		assertEquals("/user/Me myself and I/dest2", accessor.getDestination());
	}

	@Test
	public void sendToUserDefaultDestination() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertNull(accessor.getSessionId());
		assertNull(accessor.getSubscriptionId());
		assertEquals("/user/" + user.getName() + "/queue/dest", accessor.getDestination());
	}

	@Test
	public void sendToUserDefaultDestinationWhenUsingDotPathSeparator() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage("sess1", "sub1", "/app/", "dest.foo.bar", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("/user/" + user.getName() + "/queue/dest.foo.bar", accessor.getDestination());
	}

	@Test
	public void sendToUserDefaultDestinationSingleSession() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserSingleSessionDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/user/" + user.getName() + "/queue/dest", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
	}

	@Test
	public void sendToUserSessionWithoutUserName() throws Exception {

		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("/user/sess1/dest1", accessor.getDestination());
		assertEquals("sess1", accessor.getSessionId());

		accessor = getCapturedAccessor(1);
		assertEquals("/user/sess1/dest2", accessor.getDestination());
		assertEquals("sess1", accessor.getSessionId());
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

	private SimpMessageHeaderAccessor getCapturedAccessor(int index) {
		Message<?> message = this.messageCaptor.getAllValues().get(index);
		return MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
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

	@SuppressWarnings("unused")
	public String handleNoAnnotations() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendTo
	public String handleAndSendToDefaultDestination() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendTo({"/dest1", "/dest2"})
	public String handleAndSendTo() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendTo("/topic/chat.message.filtered.{roomName}")
	public String handleAndSendToWithPlaceholders() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendToUser
	public String handleAndSendToUserDefaultDestination() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendToUser(broadcast=false)
	public String handleAndSendToUserDefaultDestinationSingleSession() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendToUser({"/dest1", "/dest2"})
	public String handleAndSendToUser() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	@SendToUser(value={"/dest1", "/dest2"}, broadcast=false)
	public String handleAndSendToUserSingleSession() {
		return PAYLOAD;
	}

}
