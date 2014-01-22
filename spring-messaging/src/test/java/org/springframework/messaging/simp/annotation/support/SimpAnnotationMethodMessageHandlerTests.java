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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class SimpAnnotationMethodMessageHandlerTests {

	private TestSimpAnnotationMethodMessageHandler messageHandler;

	private TestController testController;


	@Before
	public void setup() {
		SubscribableChannel channel = Mockito.mock(SubscribableChannel.class);
		SimpMessageSendingOperations brokerTemplate = new SimpMessagingTemplate(channel);
		this.messageHandler = new TestSimpAnnotationMethodMessageHandler(brokerTemplate, channel, channel);
		this.messageHandler.setApplicationContext(new StaticApplicationContext());
		this.messageHandler.setValidator(new StringNotEmptyValidator());
		this.messageHandler.afterPropertiesSet();

		testController = new TestController();
		this.messageHandler.registerHandler(testController);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void headerArgumentResolution() {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setDestination("/pre/headers");
		headers.setHeader("foo", "bar");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.handleMessage(message);

		assertEquals("headers", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("bar", ((Map<String, Object>) this.testController.arguments.get("headers")).get("foo"));
	}

	@Test
	public void messageMappingDestinationVariableResolution() {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setDestination("/pre/message/bar/value");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.handleMessage(message);

		assertEquals("messageMappingDestinationVariable", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("value", this.testController.arguments.get("name"));
	}

	@Test
	public void subscribeEventDestinationVariableResolution() {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		headers.setDestination("/pre/sub/bar/value");
		Message<?> message = MessageBuilder.withPayload(new byte[0])
				.copyHeaders(headers.toMap()).build();
		this.messageHandler.handleMessage(message);

		assertEquals("subscribeEventDestinationVariable", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("value", this.testController.arguments.get("name"));
	}

	@Test
	public void simpleBinding() {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setDestination("/pre/binding/id/12");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.handleMessage(message);

		assertEquals("simpleBinding", this.testController.method);
		assertTrue("should be bound to type long", this.testController.arguments.get("id") instanceof Long);
		assertEquals(12L, this.testController.arguments.get("id"));
	}

	@Test
	public void validationError() {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setDestination("/pre/validation/payload");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.handleMessage(message);
		assertEquals("handleValidationException", this.testController.method);
	}


	private static class TestSimpAnnotationMethodMessageHandler extends SimpAnnotationMethodMessageHandler {

		public TestSimpAnnotationMethodMessageHandler(SimpMessageSendingOperations brokerTemplate,
				SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {

			super(clientInboundChannel, clientOutboundChannel, brokerTemplate);
		}

		public void registerHandler(Object handler) {
			super.detectHandlerMethods(handler);
		}
	}


	@Controller
	@MessageMapping("/pre")
	private static class TestController {

		private String method;

		private Map<String, Object> arguments = new LinkedHashMap<String, Object>();


		@MessageMapping("/headers")
		public void headers(@Header String foo, @Headers Map<String, Object> headers) {
			this.method = "headers";
			this.arguments.put("foo", foo);
			this.arguments.put("headers", headers);
		}

		@MessageMapping("/message/{foo}/{name}")
		public void messageMappingDestinationVariable(@DestinationVariable("foo") String param1,
				@DestinationVariable("name") String param2) {
			this.method = "messageMappingDestinationVariable";
			this.arguments.put("foo", param1);
			this.arguments.put("name", param2);
		}

		@SubscribeMapping("/sub/{foo}/{name}")
		public void subscribeEventDestinationVariable(@DestinationVariable("foo") String param1,
				@DestinationVariable("name") String param2) {
			this.method = "subscribeEventDestinationVariable";
			this.arguments.put("foo", param1);
			this.arguments.put("name", param2);
		}

		@MessageMapping("/binding/id/{id}")
		public void simpleBinding(@DestinationVariable("id") Long id) {
			this.method = "simpleBinding";
			this.arguments.put("id", id);
		}

		@MessageMapping("/validation/payload")
		public void payloadValidation(@Validated @Payload String payload) {
			this.method = "payloadValidation";
			this.arguments.put("message", payload);
		}

		@MessageExceptionHandler(MethodArgumentNotValidException.class)
		public void handleValidationException() {
			this.method = "handleValidationException";
		}
	}

	private static class StringNotEmptyValidator implements Validator {
		@Override
		public boolean supports(Class<?> clazz) {
			return String.class.isAssignableFrom(clazz);
		}
		@Override
		public void validate(Object target, Errors errors) {
			String value = (String) target;
			if (StringUtils.isEmpty(value.toString())) {
				errors.reject("empty value");
			}
		}
	}

}
