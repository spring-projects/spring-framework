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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.SendTo;
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
import static org.springframework.messaging.handler.DestinationPatternsMessageCondition.*;
import static org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver.*;
import static org.springframework.messaging.support.MessageHeaderAccessor.*;

/**
 * Test fixture for {@link SendToMethodReturnValueHandlerTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 */
public class SendToMethodReturnValueHandlerTests {

	private static final MimeType MIME_TYPE = new MimeType("text", "plain", StandardCharsets.UTF_8);

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
	private MethodParameter defaultNoAnnotation;
	private MethodParameter defaultEmptyAnnotation;
	private MethodParameter defaultOverrideAnnotation;
	private MethodParameter userDefaultNoAnnotation;
	private MethodParameter userDefaultEmptyAnnotation;
	private MethodParameter userDefaultOverrideAnnotation;


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

		method = SendToTestBean.class.getDeclaredMethod("handleNoAnnotation");
		this.defaultNoAnnotation = new SynthesizingMethodParameter(method, -1);

		method = SendToTestBean.class.getDeclaredMethod("handleAndSendToDefaultDestination");
		this.defaultEmptyAnnotation = new SynthesizingMethodParameter(method, -1);

		method = SendToTestBean.class.getDeclaredMethod("handleAndSendToOverride");
		this.defaultOverrideAnnotation = new SynthesizingMethodParameter(method, -1);

		method = SendToUserTestBean.class.getDeclaredMethod("handleNoAnnotation");
		this.userDefaultNoAnnotation = new SynthesizingMethodParameter(method, -1);

		method = SendToUserTestBean.class.getDeclaredMethod("handleAndSendToDefaultDestination");
		this.userDefaultEmptyAnnotation = new SynthesizingMethodParameter(method, -1);

		method = SendToUserTestBean.class.getDeclaredMethod("handleAndSendToOverride");
		this.userDefaultOverrideAnnotation = new SynthesizingMethodParameter(method, -1);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.sendToReturnType));
		assertTrue(this.handler.supportsReturnType(this.sendToUserReturnType));
		assertFalse(this.handler.supportsReturnType(this.noAnnotationsReturnType));
		assertTrue(this.handlerAnnotationNotRequired.supportsReturnType(this.noAnnotationsReturnType));

		assertTrue(this.handler.supportsReturnType(this.defaultNoAnnotation));
		assertTrue(this.handler.supportsReturnType(this.defaultEmptyAnnotation));
		assertTrue(this.handler.supportsReturnType(this.defaultOverrideAnnotation));

		assertTrue(this.handler.supportsReturnType(this.userDefaultNoAnnotation));
		assertTrue(this.handler.supportsReturnType(this.userDefaultEmptyAnnotation));
		assertTrue(this.handler.supportsReturnType(this.userDefaultOverrideAnnotation));
	}

	@Test
	public void sendToNoAnnotations() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", "/app", "/dest", null);
		this.handler.handleReturnValue(PAYLOAD, this.noAnnotationsReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.noAnnotationsReturnType, sessionId, 0, "/topic/dest");
	}

	@Test
	public void sendTo() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());
		assertResponse(this.sendToReturnType, sessionId, 0, "/dest1");
		assertResponse(this.sendToReturnType, sessionId, 1, "/dest2");
	}

	@Test
	public void sendToDefaultDestination() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", "/app", "/dest", null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.sendToDefaultDestReturnType, sessionId, 0, "/topic/dest");
	}

	@Test
	public void sendToClassDefaultNoAnnotation() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.defaultNoAnnotation, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.defaultNoAnnotation, sessionId, 0, "/dest-default");
	}

	@Test
	public void sendToClassDefaultEmptyAnnotation() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.defaultEmptyAnnotation, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.defaultEmptyAnnotation, sessionId, 0, "/dest-default");
	}

	@Test
	public void sendToClassDefaultOverride() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.defaultOverrideAnnotation, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());
		assertResponse(this.defaultOverrideAnnotation, sessionId, 0, "/dest3");
		assertResponse(this.defaultOverrideAnnotation, sessionId, 1, "/dest4");
	}

	@Test
	public void sendToUserClassDefaultNoAnnotation() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.userDefaultNoAnnotation, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.userDefaultNoAnnotation, sessionId, 0, "/user/sess1/dest-default");
	}

	@Test
	public void sendToUserClassDefaultEmptyAnnotation() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.userDefaultEmptyAnnotation, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(this.userDefaultEmptyAnnotation, sessionId, 0, "/user/sess1/dest-default");
	}

	@Test
	public void sendToUserClassDefaultOverride() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.userDefaultOverrideAnnotation, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());
		assertResponse(this.userDefaultOverrideAnnotation, sessionId, 0, "/user/sess1/dest3");
		assertResponse(this.userDefaultOverrideAnnotation, sessionId, 1, "/user/sess1/dest4");
	}

	@Test // SPR-14238
	public void sendToUserWithSendToDefaultOverride() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Class<?> clazz = SendToUserWithSendToOverrideTestBean.class;
		Method method = clazz.getDeclaredMethod("handleAndSendToDefaultDestination");
		MethodParameter parameter = new SynthesizingMethodParameter(method, -1);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, parameter, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());
		assertResponse(parameter, sessionId, 0, "/user/sess1/dest-default");
	}

	@Test // SPR-14238
	public void sendToUserWithSendToOverride() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Class<?> clazz = SendToUserWithSendToOverrideTestBean.class;
		Method method = clazz.getDeclaredMethod("handleAndSendToOverride");
		MethodParameter parameter = new SynthesizingMethodParameter(method, -1);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, parameter, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());
		assertResponse(parameter, sessionId, 0, "/dest3");
		assertResponse(parameter, sessionId, 1, "/dest4");
	}


	private void assertResponse(MethodParameter methodParameter, String sessionId,
			int index, String destination) {

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(index);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals(destination, accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(methodParameter, accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
	}

	@Test
	public void sendToDefaultDestinationWhenUsingDotPathSeparator() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Message<?> inputMessage = createMessage("sess1", "sub1", "/app/", "dest.foo.bar", null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals("/topic/dest.foo.bar", accessor.getDestination());
	}

	@Test
	public void testHeadersToSend() throws Exception {
		Message<?> message = createMessage("sess1", "sub1", "/app", "/dest", null);

		SimpMessageSendingOperations messagingTemplate = Mockito.mock(SimpMessageSendingOperations.class);
		SendToMethodReturnValueHandler handler = new SendToMethodReturnValueHandler(messagingTemplate, false);

		handler.handleReturnValue(PAYLOAD, this.noAnnotationsReturnType, message);

		ArgumentCaptor<MessageHeaders> captor = ArgumentCaptor.forClass(MessageHeaders.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/dest"), eq(PAYLOAD), captor.capture());

		MessageHeaders headers = captor.getValue();
		SimpMessageHeaderAccessor accessor = getAccessor(headers, SimpMessageHeaderAccessor.class);
		assertNotNull(accessor);
		assertTrue(accessor.isMutable());
		assertEquals("sess1", accessor.getSessionId());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.noAnnotationsReturnType,
				accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
	}

	@Test
	public void sendToUser() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, user);
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
		accessor.setHeader(DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
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
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserSingleSessionReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertEquals("/user/" + user.getName() + "/dest1", accessor.getDestination());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.sendToUserSingleSessionReturnType,
				accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));

		accessor = getCapturedAccessor(1);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/user/" + user.getName() + "/dest2", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.sendToUserSingleSessionReturnType,
				accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
	}

	@Test
	public void sendToUserWithUserNameProvider() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new UniqueUser();
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, user);
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
		Message<?> inputMessage = createMessage(sessionId, "sub1", "/app", "/dest", user);
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
		Message<?> inputMessage = createMessage("sess1", "sub1", "/app/", "dest.foo.bar", user);
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
		Message<?> message = createMessage(sessionId, "sub1", "/app", "/dest", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserSingleSessionDefaultDestReturnType, message);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertEquals(sessionId, accessor.getSessionId());
		assertEquals("/user/" + user.getName() + "/queue/dest", accessor.getDestination());
		assertEquals(MIME_TYPE, accessor.getContentType());
		assertNull("Subscription id should not be copied", accessor.getSubscriptionId());
		assertEquals(this.sendToUserSingleSessionDefaultDestReturnType,
				accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER));
	}

	@Test
	public void sendToUserSessionWithoutUserName() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
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
		Message<?> inputMessage = createMessage(sessionId, "sub1", "/app", "/dest", null);
		this.jsonHandler.handleReturnValue(handleAndSendToJsonView(), this.jsonViewReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		Message<?> message = this.messageCaptor.getValue();
		assertNotNull(message);

		String bytes = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
		assertEquals("{\"withView1\":\"with\"}", bytes);
	}


	private Message<?> createMessage(String sessId, String subsId, String destPrefix, String dest, Principal user) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
		headerAccessor.setSessionId(sessId);
		headerAccessor.setSubscriptionId(subsId);
		if (dest != null && destPrefix != null) {
			headerAccessor.setDestination(destPrefix + dest);
			headerAccessor.setHeader(LOOKUP_DESTINATION_HEADER, dest);
		}
		if (user != null) {
			headerAccessor.setUser(user);
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

	@SendTo
	@Retention(RetentionPolicy.RUNTIME)
	@interface MySendTo {

		@AliasFor(annotation = SendTo.class, attribute = "value")
		String[] dest();
	}

	@SendToUser
	@Retention(RetentionPolicy.RUNTIME)
	@interface MySendToUser {

		@AliasFor(annotation = SendToUser.class, attribute = "destinations")
		String[] dest();
	}


	@SuppressWarnings("unused")
	String handleNoAnnotations() {
		return PAYLOAD;
	}

	@SendTo @SuppressWarnings("unused")
	String handleAndSendToDefaultDestination() {
		return PAYLOAD;
	}

	@SendTo({"/dest1", "/dest2"}) @SuppressWarnings("unused")
	String handleAndSendTo() {
		return PAYLOAD;
	}

	@SendTo("/topic/chat.message.filtered.{roomName}") @SuppressWarnings("unused")
	String handleAndSendToWithPlaceholders() {
		return PAYLOAD;
	}

	@SendToUser @SuppressWarnings("unused")
	String handleAndSendToUserDefaultDestination() {
		return PAYLOAD;
	}

	@SendToUser(broadcast = false) @SuppressWarnings("unused")
	String handleAndSendToUserDefaultDestinationSingleSession() {
		return PAYLOAD;
	}

	@SendToUser({"/dest1", "/dest2"}) @SuppressWarnings("unused")
	String handleAndSendToUser() {
		return PAYLOAD;
	}

	@SendToUser(destinations = { "/dest1", "/dest2" }, broadcast = false) @SuppressWarnings("unused")
	String handleAndSendToUserSingleSession() {
		return PAYLOAD;
	}

	@JsonView(MyJacksonView1.class) @SuppressWarnings("unused")
	JacksonViewBean handleAndSendToJsonView() {
		JacksonViewBean payload = new JacksonViewBean();
		payload.setWithView1("with");
		payload.setWithView2("with");
		payload.setWithoutView("without");
		return payload;
	}


	@MySendTo(dest = "/dest-default") @SuppressWarnings("unused")
	private static class SendToTestBean {

		String handleNoAnnotation() {
			return PAYLOAD;
		}

		@SendTo
		String handleAndSendToDefaultDestination() {
			return PAYLOAD;
		}

		@MySendTo(dest = {"/dest3", "/dest4"})
		String handleAndSendToOverride() {
			return PAYLOAD;
		}
	}

	@MySendToUser(dest = "/dest-default") @SuppressWarnings("unused")
	private static class SendToUserTestBean {

		String handleNoAnnotation() {
			return PAYLOAD;
		}

		@SendToUser
		String handleAndSendToDefaultDestination() {
			return PAYLOAD;
		}

		@MySendToUser(dest = {"/dest3", "/dest4"})
		String handleAndSendToOverride() {
			return PAYLOAD;
		}
	}

	@MySendToUser(dest = "/dest-default") @SuppressWarnings("unused")
	private static class SendToUserWithSendToOverrideTestBean {

		@SendTo
		String handleAndSendToDefaultDestination() {
			return PAYLOAD;
		}

		@MySendTo(dest = {"/dest3", "/dest4"})
		String handleAndSendToOverride() {
			return PAYLOAD;
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
