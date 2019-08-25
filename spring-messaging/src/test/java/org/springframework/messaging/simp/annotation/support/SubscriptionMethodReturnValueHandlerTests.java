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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link SubscriptionMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
@ExtendWith(MockitoExtension.class)
public class SubscriptionMethodReturnValueHandlerTests {

	public static final MimeType MIME_TYPE = new MimeType("text", "plain", StandardCharsets.UTF_8);

	private static final String PAYLOAD = "payload";


	@Mock
	private MessageChannel messageChannel;

	@Captor
	private ArgumentCaptor<Message<?>> messageCaptor;

	private SubscriptionMethodReturnValueHandler handler;

	private SubscriptionMethodReturnValueHandler jsonHandler;

	private MethodParameter subscribeEventReturnType;

	private MethodParameter subscribeEventSendToReturnType;

	private MethodParameter messageMappingReturnType;

	private MethodParameter subscribeEventJsonViewReturnType;


	@BeforeEach
	public void setup() throws Exception {
		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		messagingTemplate.setMessageConverter(new StringMessageConverter());
		this.handler = new SubscriptionMethodReturnValueHandler(messagingTemplate);

		SimpMessagingTemplate jsonMessagingTemplate = new SimpMessagingTemplate(this.messageChannel);
		jsonMessagingTemplate.setMessageConverter(new MappingJackson2MessageConverter());
		this.jsonHandler = new SubscriptionMethodReturnValueHandler(jsonMessagingTemplate);

		Method method = this.getClass().getDeclaredMethod("getData");
		this.subscribeEventReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("getDataAndSendTo");
		this.subscribeEventSendToReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("handle");
		this.messageMappingReturnType = new MethodParameter(method, -1);

		method = this.getClass().getDeclaredMethod("getJsonView");
		this.subscribeEventJsonViewReturnType = new MethodParameter(method, -1);
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(this.subscribeEventReturnType)).isTrue();
		assertThat(this.handler.supportsReturnType(this.subscribeEventSendToReturnType)).isFalse();
		assertThat(this.handler.supportsReturnType(this.messageMappingReturnType)).isFalse();
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
		assertThat(this.messageCaptor.getValue()).isNotNull();

		Message<?> message = this.messageCaptor.getValue();
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);

		assertThat(headerAccessor.getId()).as("SimpMessageHeaderAccessor should have disabled id").isNull();
		assertThat(headerAccessor.getTimestamp()).as("SimpMessageHeaderAccessor should have disabled timestamp").isNull();
		assertThat(headerAccessor.getSessionId()).isEqualTo(sessionId);
		assertThat(headerAccessor.getSubscriptionId()).isEqualTo(subscriptionId);
		assertThat(headerAccessor.getDestination()).isEqualTo(destination);
		assertThat(headerAccessor.getContentType()).isEqualTo(MIME_TYPE);
		assertThat(headerAccessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.subscribeEventReturnType);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testHeadersPassedToMessagingTemplate() throws Exception {
		String sessionId = "sess1";
		String subscriptionId = "subs1";
		String destination = "/dest";
		Message<?> inputMessage = createInputMessage(sessionId, subscriptionId, destination, null);

		MessageSendingOperations messagingTemplate = mock(MessageSendingOperations.class);
		SubscriptionMethodReturnValueHandler handler = new SubscriptionMethodReturnValueHandler(messagingTemplate);

		handler.handleReturnValue(PAYLOAD, this.subscribeEventReturnType, inputMessage);

		ArgumentCaptor<MessageHeaders> captor = ArgumentCaptor.forClass(MessageHeaders.class);
		verify(messagingTemplate).convertAndSend(eq("/dest"), eq(PAYLOAD), captor.capture());

		SimpMessageHeaderAccessor headerAccessor =
				MessageHeaderAccessor.getAccessor(captor.getValue(), SimpMessageHeaderAccessor.class);

		assertThat(headerAccessor).isNotNull();
		assertThat(headerAccessor.isMutable()).isTrue();
		assertThat(headerAccessor.getSessionId()).isEqualTo(sessionId);
		assertThat(headerAccessor.getSubscriptionId()).isEqualTo(subscriptionId);
		assertThat(headerAccessor.getHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER)).isEqualTo(this.subscribeEventReturnType);
	}

	@Test
	public void testJsonView() throws Exception {
		given(this.messageChannel.send(any(Message.class))).willReturn(true);

		String sessionId = "sess1";
		String subscriptionId = "subs1";
		String destination = "/dest";
		Message<?> inputMessage = createInputMessage(sessionId, subscriptionId, destination, null);

		this.jsonHandler.handleReturnValue(getJsonView(), this.subscribeEventJsonViewReturnType, inputMessage);

		verify(this.messageChannel).send(this.messageCaptor.capture());
		Message<?> message = this.messageCaptor.getValue();
		assertThat(message).isNotNull();

		assertThat(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"withView1\":\"with\"}");
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

	@SubscribeMapping("/jsonview")	// not needed for the tests but here for completeness
	@JsonView(MyJacksonView1.class)
	public JacksonViewBean getJsonView() {
		JacksonViewBean payload = new JacksonViewBean();
		payload.setWithView1("with");
		payload.setWithView2("with");
		payload.setWithoutView("without");
		return payload;
	}


	private interface MyJacksonView1 {};
	private interface MyJacksonView2 {};

	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;

		public String getWithView1() {
			return withView1;
		}

		public void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}

}
