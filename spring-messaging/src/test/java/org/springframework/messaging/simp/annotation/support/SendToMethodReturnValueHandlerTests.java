/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AliasFor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link SendToMethodReturnValueHandlerTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
public class SendToMethodReturnValueHandlerTests {

	private static final MimeType MIME_TYPE = new MimeType("text", "plain", StandardCharsets.UTF_8);

	private static final String PAYLOAD = "payload";


	@Mock
	private MessageChannel messageChannel;

	@Captor
	private ArgumentCaptor<Message<?>> messageCaptor;

	private SendToMethodReturnValueHandler handler;

	private SendToMethodReturnValueHandler handlerAnnotationNotRequired;

	private SendToMethodReturnValueHandler jsonHandler;

	private MethodParameter noAnnotationsReturnType = param("handleNoAnnotations");
	private MethodParameter sendToReturnType = param("handleAndSendTo");
	private MethodParameter sendToDefaultDestReturnType = param("handleAndSendToDefaultDest");
	private MethodParameter sendToWithPlaceholdersReturnType = param("handleAndSendToWithPlaceholders");
	private MethodParameter sendToUserReturnType = param("handleAndSendToUser");
	private MethodParameter sendToUserInSessionReturnType = param("handleAndSendToUserInSession");
	private MethodParameter sendToSendToUserReturnType = param("handleAndSendToAndSendToUser");
	private MethodParameter sendToUserDefaultDestReturnType = param("handleAndSendToUserDefaultDest");
	private MethodParameter sendToUserInSessionDefaultDestReturnType = param("handleAndSendToUserDefaultDestInSession");
	private MethodParameter jsonViewReturnType = param("handleAndSendToJsonView");
	private MethodParameter defaultNoAnnotation = param(SendToTestBean.class, "handleNoAnnotation");
	private MethodParameter defaultEmptyAnnotation = param(SendToTestBean.class, "handleAndSendToDefaultDest");
	private MethodParameter defaultOverrideAnnotation = param(SendToTestBean.class, "handleAndSendToOverride");
	private MethodParameter userDefaultNoAnnotation = param(SendToUserTestBean.class, "handleNoAnnotation");
	private MethodParameter userDefaultEmptyAnnotation = param(SendToUserTestBean.class, "handleAndSendToDefaultDest");
	private MethodParameter userDefaultOverrideAnnotation = param(SendToUserTestBean.class, "handleAndSendToOverride");

	private MethodParameter param(String methodName) {
		return param(getClass(), methodName);
	}

	private static MethodParameter param(Class<?> clazz, String methodName) {
		try {
			return new SynthesizingMethodParameter(clazz.getDeclaredMethod(methodName), -1);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("No such method", ex);
		}
	}


	@BeforeEach
	public void setup() throws Exception {
		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());
		this.handler = new SendToMethodReturnValueHandler(messagingTemplate, true);
		this.handlerAnnotationNotRequired = new SendToMethodReturnValueHandler(messagingTemplate, false);

		SimpMessagingTemplate jsonMessagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		jsonMessagingTemplate.setMessageConverter(new MappingJackson2MessageConverter());
		this.jsonHandler = new SendToMethodReturnValueHandler(jsonMessagingTemplate, true);
	}

	@Test
	public void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(this.sendToReturnType)).isTrue();
		assertThat(this.handler.supportsReturnType(this.sendToUserReturnType)).isTrue();
		assertThat(this.handler.supportsReturnType(this.noAnnotationsReturnType)).isFalse();
		assertThat(this.handlerAnnotationNotRequired.supportsReturnType(this.noAnnotationsReturnType)).isTrue();

		assertThat(this.handler.supportsReturnType(this.defaultNoAnnotation)).isTrue();
		assertThat(this.handler.supportsReturnType(this.defaultEmptyAnnotation)).isTrue();
		assertThat(this.handler.supportsReturnType(this.defaultOverrideAnnotation)).isTrue();

		assertThat(this.handler.supportsReturnType(this.userDefaultNoAnnotation)).isTrue();
		assertThat(this.handler.supportsReturnType(this.userDefaultEmptyAnnotation)).isTrue();
		assertThat(this.handler.supportsReturnType(this.userDefaultOverrideAnnotation)).isTrue();
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
		assertThat(accessor.getSessionId()).isEqualTo(sessionId);
		assertThat(accessor.getDestination()).isEqualTo(destination);
		assertThat(accessor.getContentType()).isEqualTo(MIME_TYPE);
		assertThat(accessor.getSubscriptionId()).as("Subscription id should not be copied").isNull();
		assertThat(accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(methodParameter);
	}

	@Test
	public void sendToDefaultDestinationWhenUsingDotPathSeparator() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		Message<?> inputMessage = createMessage("sess1", "sub1", "/app/", "dest.foo.bar", null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getDestination()).isEqualTo("/topic/dest.foo.bar");
	}

	@Test
	public void testHeadersToSend() throws Exception {
		Message<?> message = createMessage("sess1", "sub1", "/app", "/dest", null);

		SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
		SendToMethodReturnValueHandler handler = new SendToMethodReturnValueHandler(messagingTemplate, false);

		handler.handleReturnValue(PAYLOAD, this.noAnnotationsReturnType, message);

		ArgumentCaptor<MessageHeaders> captor = ArgumentCaptor.forClass(MessageHeaders.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/dest"), eq(PAYLOAD), captor.capture());

		MessageHeaders headers = captor.getValue();
		SimpMessageHeaderAccessor accessor =
				MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
		assertThat(accessor).isNotNull();
		assertThat(accessor.isMutable()).isTrue();
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getSubscriptionId()).as("Subscription id should not be copied").isNull();
		assertThat(accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.noAnnotationsReturnType);
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
		assertThat(accessor.getSessionId()).isNull();
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest1"));

		accessor = getCapturedAccessor(1);
		assertThat(accessor.getSessionId()).isNull();
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest2"));
	}

	@Test
	public void sendToAndSendToUser() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToSendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(4)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getSessionId()).isNull();
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest1"));

		accessor = getCapturedAccessor(1);
		assertThat(accessor.getSessionId()).isNull();
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest2"));

		accessor = getCapturedAccessor(2);
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo("/dest1");

		accessor = getCapturedAccessor(3);
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo("/dest2");
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
		assertThat(actual.getSessionId()).isEqualTo(sessionId);
		assertThat(actual.getDestination()).isEqualTo("/topic/chat.message.filtered.roomA");
	}

	@Test
	public void sendToUserSingleSession() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserInSessionReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getSessionId()).isEqualTo(sessionId);
		assertThat(accessor.getContentType()).isEqualTo(MIME_TYPE);
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest1"));
		assertThat(accessor.getSubscriptionId()).as("Subscription id should not be copied").isNull();
		assertThat(accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.sendToUserInSessionReturnType);

		accessor = getCapturedAccessor(1);
		assertThat(accessor.getSessionId()).isEqualTo(sessionId);
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/dest2"));
		assertThat(accessor.getContentType()).isEqualTo(MIME_TYPE);
		assertThat(accessor.getSubscriptionId()).as("Subscription id should not be copied").isNull();
		assertThat(accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.sendToUserInSessionReturnType);
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
		assertThat(accessor.getDestination()).isEqualTo("/user/Me myself and I/dest1");

		accessor = getCapturedAccessor(1);
		assertThat(accessor.getDestination()).isEqualTo("/user/Me myself and I/dest2");
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
		assertThat(accessor.getSessionId()).isNull();
		assertThat(accessor.getSubscriptionId()).isNull();
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/queue/dest"));
	}

	@Test
	public void sendToUserDefaultDestinationWhenUsingDotPathSeparator() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		TestUser user = new TestUser();
		Message<?> inputMessage = createMessage("sess1", "sub1", "/app/", "dest.foo.bar", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserDefaultDestReturnType, inputMessage);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/queue/dest.foo.bar"));
	}

	@Test
	public void sendToUserDefaultDestinationSingleSession() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		TestUser user = new TestUser();
		Message<?> message = createMessage(sessionId, "sub1", "/app", "/dest", user);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserInSessionDefaultDestReturnType, message);

		verify(this.messageChannel, times(1)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getSessionId()).isEqualTo(sessionId);
		assertThat(accessor.getDestination()).isEqualTo(("/user/" + user.getName() + "/queue/dest"));
		assertThat(accessor.getContentType()).isEqualTo(MIME_TYPE);
		assertThat(accessor.getSubscriptionId()).as("Subscription id should not be copied").isNull();
		assertThat(accessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.sendToUserInSessionDefaultDestReturnType);
	}

	@Test
	public void sendToUserSessionWithoutUserName() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", null, null, null);
		this.handler.handleReturnValue(PAYLOAD, this.sendToUserReturnType, inputMessage);

		verify(this.messageChannel, times(2)).send(this.messageCaptor.capture());

		SimpMessageHeaderAccessor accessor = getCapturedAccessor(0);
		assertThat(accessor.getDestination()).isEqualTo("/user/sess1/dest1");
		assertThat(accessor.getSessionId()).isEqualTo("sess1");

		accessor = getCapturedAccessor(1);
		assertThat(accessor.getDestination()).isEqualTo("/user/sess1/dest2");
		assertThat(accessor.getSessionId()).isEqualTo("sess1");
	}

	@Test
	public void jsonView() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		Message<?> inputMessage = createMessage(sessionId, "sub1", "/app", "/dest", null);
		this.jsonHandler.handleReturnValue(handleAndSendToJsonView(), this.jsonViewReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		Message<?> message = this.messageCaptor.getValue();
		assertThat(message).isNotNull();

		String bytes = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
		assertThat(bytes).isEqualTo("{\"withView1\":\"with\"}");
	}


	private Message<?> createMessage(String sessId, String subsId, String destPrefix, String dest, Principal user) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
		headerAccessor.setSessionId(sessId);
		headerAccessor.setSubscriptionId(subsId);
		if (dest != null && destPrefix != null) {
			headerAccessor.setDestination(destPrefix + dest);
			headerAccessor.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, dest);
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


	@SuppressWarnings("unused")
	String handleNoAnnotations() {
		return PAYLOAD;
	}

	@SendTo
	@SuppressWarnings("unused")
	String handleAndSendToDefaultDest() {
		return PAYLOAD;
	}

	@SendTo({"/dest1", "/dest2"})
	@SuppressWarnings("unused")
	String handleAndSendTo() {
		return PAYLOAD;
	}

	@SendTo("/topic/chat.message.filtered.{roomName}")
	@SuppressWarnings("unused")
	String handleAndSendToWithPlaceholders() {
		return PAYLOAD;
	}

	@SendToUser
	@SuppressWarnings("unused")
	String handleAndSendToUserDefaultDest() {
		return PAYLOAD;
	}

	@SendToUser(broadcast = false)
	@SuppressWarnings("unused")
	String handleAndSendToUserDefaultDestInSession() {
		return PAYLOAD;
	}

	@SendToUser({"/dest1", "/dest2"})
	@SuppressWarnings("unused")
	String handleAndSendToUser() {
		return PAYLOAD;
	}

	@SendToUser(destinations = { "/dest1", "/dest2" }, broadcast = false)
	@SuppressWarnings("unused")
	String handleAndSendToUserInSession() {
		return PAYLOAD;
	}

	@SendTo({"/dest1", "/dest2"})
	@SendToUser({"/dest1", "/dest2"})
	@SuppressWarnings("unused")
	String handleAndSendToAndSendToUser() {
		return PAYLOAD;
	}

	@JsonView(MyJacksonView1.class)
	@SuppressWarnings("unused")
	JacksonViewBean handleAndSendToJsonView() {
		JacksonViewBean payload = new JacksonViewBean();
		payload.setWithView1("with");
		payload.setWithView2("with");
		payload.setWithoutView("without");
		return payload;
	}


	private static class TestUser implements Principal {

		@Override
		public String getName() {
			return "joe";
		}

		@Override
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


	@MySendTo(dest = "/dest-default") @SuppressWarnings("unused")
	private static class SendToTestBean {

		String handleNoAnnotation() {
			return PAYLOAD;
		}

		@SendTo
		String handleAndSendToDefaultDest() {
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
		String handleAndSendToDefaultDest() {
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
