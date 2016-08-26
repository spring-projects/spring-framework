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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.security.auth.Subject;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link SendToMethodReturnValueHandlerTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class SendToMethodReturnValueHandlerTests {

	private static final MimeType MIME_TYPE = new MimeType("text", "plain", Charset.forName("UTF-8"));

	private static final String PAYLOAD = "payload";


	private SendToMethodReturnValueHandler handler;

	private SendToMethodReturnValueHandler handlerAnnotationNotRequired;

	private SendToMethodReturnValueHandler jsonHandler;

	@Mock private MessageChannel messageChannel;

	@Captor private ArgumentCaptor<Message<?>> messageCaptor;

	private MethodParameter noAnnotationsReturnType;
	private MethodParameter sendToReturnType;
	private MethodParameter sendToDefaultDestReturnType;
	private MethodParameter sendToWithPlaceholdersReturnType;
	private MethodParameter sendToUserReturnType;
	private MethodParameter sendToUserSingleSessionReturnType;
	private MethodParameter sendToUserDefaultDestReturnType;
	private MethodParameter sendToUserSingleSessionDefaultDestReturnType;
	private MethodParameter jsonViewReturnType;


	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);

		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());
		this.handler = new SendToMethodReturnValueHandler(messagingTemplate, true);
		this.handlerAnnotationNotRequired = new SendToMethodReturnValueHandler(messagingTemplate, false);

		SimpMessagingTemplate jsonMessagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		jsonMessagingTemplate.setMessageConverter(new MappingJackson2MessageConverter());
		this.jsonHandler = new SendToMethodReturnValueHandler(jsonMessagingTemplate, true);

		Method method = getClass().getDeclaredMethod("handleNoAnnotations");
		this.noAnnotationsReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToDefaultDestination");
		this.sendToDefaultDestReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendTo");
		this.sendToReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToWithPlaceholders");
		this.sendToWithPlaceholdersReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToUser");
		this.sendToUserReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToUserSingleSession");
		this.sendToUserSingleSessionReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToUserDefaultDestination");
		this.sendToUserDefaultDestReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToUserDefaultDestinationSingleSession");
		this.sendToUserSingleSessionDefaultDestReturnType = new SynthesizingMethodParameter(method, -1);

		method = getClass().getDeclaredMethod("handleAndSendToJsonView");
		this.jsonViewReturnType = new SynthesizingMethodParameter(method, -1);
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
		assertEquals(this.noAnnotationsReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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
		assertEquals(this.sendToReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));

		accessor = getCapturedAccessor(1);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/dest2", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.sendToReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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
		assertEquals(this.sendToDefaultDestReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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
		SimpMessageHeaderAccessor accessor =
				MessageHeaderAccessor.getAccessor(messageHeaders, SimpMessageHeaderAccessor.class);
		assertNotNull(accessor);
		assertTrue(accessor.isMutable());
		assertEquals("sess1", accessor.getSessionId());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.noAnnotationsReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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

	@Test  // SPR-12170
	public void sendToWithDestinationPlaceholders() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Map<String, String> vars = new LinkedHashMap<>(1);
		vars.put("roomName", "roomA");

		String sessionId = "sess1";
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId("sub1");
		accessor.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
		Message<?> message = MessageBuilder.createMessage(PAYLOAD, accessor.getMessageHeaders());
		this.handler.handleReturnValue(PAYLOAD, this.sendToWithPlaceholdersReturnType, message);

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
		assertEquals(this.sendToUserSingleSessionReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));

		accessor = getCapturedAccessor(1);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/user/" + user.getName() + "/dest2", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.sendToUserSingleSessionReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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
		assertEquals(this.sendToUserSingleSessionDefaultDestReturnType, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
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

	@Test
	public void jsonView() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createInputMessage(sessionId, "sub1", "/app", "/dest", null);
		this.jsonHandler.handleReturnValue(handleAndSendToJsonView(), this.jsonViewReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		Message<?> message = this.messageCaptor.getValue();
		assertNotNull(message);

		assertEquals("{\"withView1\":\"with\"}", new String((byte[]) message.getPayload(), Charset.forName("UTF-8")));
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


	public String handleNoAnnotations() {
		return PAYLOAD;
	}

	@SendTo
	public String handleAndSendToDefaultDestination() {
		return PAYLOAD;
	}

	@SendTo({"/dest1", "/dest2"})
	public String handleAndSendTo() {
		return PAYLOAD;
	}

	@SendTo("/topic/chat.message.filtered.{roomName}")
	public String handleAndSendToWithPlaceholders() {
		return PAYLOAD;
	}

	@SendToUser
	public String handleAndSendToUserDefaultDestination() {
		return PAYLOAD;
	}

	@SendToUser(broadcast = false)
	public String handleAndSendToUserDefaultDestinationSingleSession() {
		return PAYLOAD;
	}

	@SendToUser({"/dest1", "/dest2"})
	public String handleAndSendToUser() {
		return PAYLOAD;
	}

	@SendToUser(destinations = { "/dest1", "/dest2" }, broadcast = false)
	public String handleAndSendToUserSingleSession() {
		return PAYLOAD;
	}

	@SendTo("/dest")
	@JsonView(MyJacksonView1.class)
	public JacksonViewBean handleAndSendToJsonView() {
		JacksonViewBean payload = new JacksonViewBean();
		payload.setWithView1("with");
		payload.setWithView2("with");
		payload.setWithoutView("without");
		return payload;
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


	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}


	@SuppressWarnings("unused")
	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;

		public String getWithView1() {
			return withView1;
		}

		void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		String getWithView2() {
			return withView2;
		}

		void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		String getWithoutView() {
			return withoutView;
		}

		void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}

}
