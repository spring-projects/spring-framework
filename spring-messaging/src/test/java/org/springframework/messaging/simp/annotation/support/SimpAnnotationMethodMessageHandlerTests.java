/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture for
 * {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class SimpAnnotationMethodMessageHandlerTests {

	private static final String TEST_INVALID_VALUE = "invalidValue";


	private TestSimpAnnotationMethodMessageHandler messageHandler;

	private TestController testController;

	@Mock
	private SubscribableChannel channel;

	@Mock
	private MessageConverter converter;

	@Captor
	private ArgumentCaptor<Object> payloadCaptor;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		SimpMessagingTemplate brokerTemplate = new SimpMessagingTemplate(this.channel);
		brokerTemplate.setMessageConverter(this.converter);

		this.messageHandler = new TestSimpAnnotationMethodMessageHandler(brokerTemplate, this.channel, this.channel);
		this.messageHandler.setApplicationContext(new StaticApplicationContext());
		this.messageHandler.setValidator(new StringTestValidator(TEST_INVALID_VALUE));
		this.messageHandler.afterPropertiesSet();

		this.testController = new TestController();
	}


	@Test
	@SuppressWarnings("unchecked")
	public void headerArgumentResolution() {
		Map<String, Object> headers = Collections.singletonMap("foo", "bar");
		Message<?> message = createMessage("/pre/headers", headers);
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("headers", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("bar", ((Map<String, Object>) this.testController.arguments.get("headers")).get("foo"));
	}

	@Test
	public void optionalHeaderArgumentResolutionWhenPresent() {
		Map<String, Object> headers = Collections.singletonMap("foo", "bar");
		Message<?> message = createMessage("/pre/optionalHeaders", headers);
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("optionalHeaders", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo1"));
		assertEquals("bar", this.testController.arguments.get("foo2"));
	}

	@Test
	public void optionalHeaderArgumentResolutionWhenNotPresent() {
		Message<?> message = createMessage("/pre/optionalHeaders");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("optionalHeaders", this.testController.method);
		assertNull(this.testController.arguments.get("foo1"));
		assertNull(this.testController.arguments.get("foo2"));
	}

	@Test
	public void messageMappingDestinationVariableResolution() {
		Message<?> message = createMessage("/pre/message/bar/value");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("messageMappingDestinationVariable", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("value", this.testController.arguments.get("name"));
	}

	@Test
	public void subscribeEventDestinationVariableResolution() {
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, "/pre/sub/bar/value", null);
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("subscribeEventDestinationVariable", this.testController.method);
		assertEquals("bar", this.testController.arguments.get("foo"));
		assertEquals("value", this.testController.arguments.get("name"));
	}

	@Test
	public void simpleBinding() {
		Message<?> message = createMessage("/pre/binding/id/12");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("simpleBinding", this.testController.method);
		assertTrue("should be bound to type long", this.testController.arguments.get("id") instanceof Long);
		assertEquals(12L, this.testController.arguments.get("id"));
	}

	@Test
	public void validationError() {
		Message<?> message = createMessage("/pre/validation/payload");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("handleValidationException", this.testController.method);
	}

	@Test
	public void exceptionWithHandlerMethodArg() {
		Message<?> message = createMessage("/pre/illegalState");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("handleExceptionWithHandlerMethodArg", this.testController.method);
		HandlerMethod handlerMethod = (HandlerMethod) this.testController.arguments.get("handlerMethod");
		assertNotNull(handlerMethod);
		assertEquals("illegalState", handlerMethod.getMethod().getName());
	}

	@Test
	public void exceptionAsCause() {
		Message<?> message = createMessage("/pre/illegalStateCause");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("handleExceptionWithHandlerMethodArg", this.testController.method);
		HandlerMethod handlerMethod = (HandlerMethod) this.testController.arguments.get("handlerMethod");
		assertNotNull(handlerMethod);
		assertEquals("illegalStateCause", handlerMethod.getMethod().getName());
	}

	@Test
	public void errorAsMessageHandlingException() {
		Message<?> message = createMessage("/pre/error");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("handleErrorWithHandlerMethodArg", this.testController.method);
		HandlerMethod handlerMethod = (HandlerMethod) this.testController.arguments.get("handlerMethod");
		assertNotNull(handlerMethod);
		assertEquals("errorAsThrowable", handlerMethod.getMethod().getName());
	}

	@Test
	public void simpScope() {
		Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();
		sessionAttributes.put("name", "value");

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setSessionId("session1");
		headers.setSessionAttributes(sessionAttributes);
		headers.setDestination("/pre/scope");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("scope", this.testController.method);
	}

	@Test
	public void dotPathSeparator() {
		DotPathSeparatorController controller = new DotPathSeparatorController();

		this.messageHandler.setPathMatcher(new AntPathMatcher("."));
		this.messageHandler.registerHandler(controller);
		this.messageHandler.setDestinationPrefixes(Arrays.asList("/app1", "/app2/"));

		Message<?> message = createMessage("/app1/pre.foo");
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("handleFoo", controller.method);

		message = createMessage("/app2/pre.foo");
		this.messageHandler.handleMessage(message);

		assertEquals("handleFoo", controller.method);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listenableFutureSuccess() {
		Message emptyMessage = (Message) MessageBuilder.withPayload(new byte[0]).build();
		given(this.channel.send(any(Message.class))).willReturn(true);
		given(this.converter.toMessage(anyObject(), any(MessageHeaders.class))).willReturn(emptyMessage);

		ListenableFutureController controller = new ListenableFutureController();
		this.messageHandler.registerHandler(controller);
		this.messageHandler.setDestinationPrefixes(Arrays.asList("/app1", "/app2/"));

		Message<?> message = createMessage("/app1/listenable-future/success");
		this.messageHandler.handleMessage(message);

		assertNotNull(controller.future);
		controller.future.run();
		verify(this.converter).toMessage(this.payloadCaptor.capture(), any(MessageHeaders.class));
		assertEquals("foo", this.payloadCaptor.getValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listenableFutureFailure() {
		Message emptyMessage = (Message) MessageBuilder.withPayload(new byte[0]).build();
		given(this.channel.send(any(Message.class))).willReturn(true);
		given(this.converter.toMessage(anyObject(), any(MessageHeaders.class))).willReturn(emptyMessage);

		ListenableFutureController controller = new ListenableFutureController();
		this.messageHandler.registerHandler(controller);
		this.messageHandler.setDestinationPrefixes(Arrays.asList("/app1", "/app2/"));

		Message<?> message = createMessage("/app1/listenable-future/failure");
		this.messageHandler.handleMessage(message);

		controller.future.run();
		assertTrue(controller.exceptionCaught);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void completableFutureSuccess() {
		Message emptyMessage = (Message) MessageBuilder.withPayload(new byte[0]).build();
		given(this.channel.send(any(Message.class))).willReturn(true);
		given(this.converter.toMessage(anyObject(), any(MessageHeaders.class))).willReturn(emptyMessage);

		CompletableFutureController controller = new CompletableFutureController();
		this.messageHandler.registerHandler(controller);
		this.messageHandler.setDestinationPrefixes(Arrays.asList("/app1", "/app2/"));

		Message<?> message = createMessage("/app1/completable-future");
		this.messageHandler.handleMessage(message);

		assertNotNull(controller.future);
		controller.future.complete("foo");
		verify(this.converter).toMessage(this.payloadCaptor.capture(), any(MessageHeaders.class));
		assertEquals("foo", this.payloadCaptor.getValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void completableFutureFailure() {
		Message emptyMessage = (Message) MessageBuilder.withPayload(new byte[0]).build();
		given(this.channel.send(any(Message.class))).willReturn(true);
		given(this.converter.toMessage(anyObject(), any(MessageHeaders.class))).willReturn(emptyMessage);

		CompletableFutureController controller = new CompletableFutureController();
		this.messageHandler.registerHandler(controller);
		this.messageHandler.setDestinationPrefixes(Arrays.asList("/app1", "/app2/"));

		Message<?> message = createMessage("/app1/completable-future");
		this.messageHandler.handleMessage(message);

		controller.future.completeExceptionally(new IllegalStateException());
		assertTrue(controller.exceptionCaught);
	}

	@Test
	public void placeholder() throws Exception {
		Message<?> message = createMessage("/pre/myValue");
		this.messageHandler.setEmbeddedValueResolver(value -> ("/${myProperty}".equals(value) ? "/myValue" : value));
		this.messageHandler.registerHandler(this.testController);
		this.messageHandler.handleMessage(message);

		assertEquals("placeholder", this.testController.method);
	}


	private Message<?> createMessage(String destination) {
		return createMessage(destination, null);
	}

	private Message<?> createMessage(String destination, Map<String, Object> headers) {
		return createMessage(SimpMessageType.MESSAGE, destination, headers);
	}

	private Message<?> createMessage(SimpMessageType messageType, String destination, Map<String, Object> headers) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
		accessor.setSessionId("session1");
		accessor.setSessionAttributes(new HashMap<>());
		accessor.setDestination(destination);
		if (headers != null) {
			for (Map.Entry<String, Object> entry : headers.entrySet()) {
				accessor.setHeader(entry.getKey(), entry.getValue());
			}
		}
		return MessageBuilder.withPayload(new byte[0]).setHeaders(accessor).build();
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

		private Map<String, Object> arguments = new LinkedHashMap<>();

		@MessageMapping("/headers")
		public void headers(@Header String foo, @Headers Map<String, Object> headers) {
			this.method = "headers";
			this.arguments.put("foo", foo);
			this.arguments.put("headers", headers);
		}

		@MessageMapping("/optionalHeaders")
		public void optionalHeaders(@Header(name="foo", required=false) String foo1, @Header("foo") Optional<String> foo2) {
			this.method = "optionalHeaders";
			this.arguments.put("foo1", foo1);
			this.arguments.put("foo2", (foo2.isPresent() ? foo2.get() : null));
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

		@MessageMapping("/illegalState")
		public void illegalState() {
			throw new IllegalStateException("my cause");
		}

		@MessageMapping("/illegalStateCause")
		public void illegalStateCause() {
			throw new RuntimeException(new IllegalStateException("my cause"));
		}

		@MessageMapping("/error")
		public void errorAsThrowable() {
			throw new Error("my cause");
		}

		@MessageExceptionHandler(MethodArgumentNotValidException.class)
		public void handleValidationException() {
			this.method = "handleValidationException";
		}

		@MessageExceptionHandler
		public void handleExceptionWithHandlerMethodArg(IllegalStateException ex, HandlerMethod handlerMethod) {
			this.method = "handleExceptionWithHandlerMethodArg";
			this.arguments.put("handlerMethod", handlerMethod);
			assertEquals("my cause", ex.getMessage());
		}

		@MessageExceptionHandler
		public void handleErrorWithHandlerMethodArg(Error ex, HandlerMethod handlerMethod) {
			this.method = "handleErrorWithHandlerMethodArg";
			this.arguments.put("handlerMethod", handlerMethod);
			assertEquals("my cause", ex.getMessage());
		}

		@MessageMapping("/scope")
		public void scope() {
			SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
			assertThat(simpAttributes.getAttribute("name"), is("value"));
			this.method = "scope";
		}

		@MessageMapping("/${myProperty}")
		public void placeholder() {
			this.method = "placeholder";
		}
	}


	@Controller
	@MessageMapping("pre")
	private static class DotPathSeparatorController {

		private String method;

		@MessageMapping("foo")
		public void handleFoo() {
			this.method = "handleFoo";
		}
	}


	@Controller
	@MessageMapping("listenable-future")
	private static class ListenableFutureController {

		private ListenableFutureTask<String> future;

		private boolean exceptionCaught = false;

		@MessageMapping("success")
		public ListenableFutureTask<String> handleListenableFuture() {
			this.future = new ListenableFutureTask<>(() -> "foo");
			return this.future;
		}

		@MessageMapping("failure")
		public ListenableFutureTask<String> handleListenableFutureException() {
			this.future = new ListenableFutureTask<>(() -> {
				throw new IllegalStateException();
			});
			return this.future;
		}

		@MessageExceptionHandler(IllegalStateException.class)
		public void handleValidationException() {
			this.exceptionCaught = true;
		}
	}


	@Controller
	private static class CompletableFutureController {

		private CompletableFuture<String> future;

		private boolean exceptionCaught = false;

		@MessageMapping("completable-future")
		public CompletableFuture<String> handleCompletableFuture() {
			this.future = new CompletableFuture<>();
			return this.future;
		}

		@MessageExceptionHandler(IllegalStateException.class)
		public void handleValidationException() {
			this.exceptionCaught = true;
		}
	}


	private static class StringTestValidator implements Validator {

		private final String invalidValue;

		public StringTestValidator(String invalidValue) {
			this.invalidValue = invalidValue;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return String.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			String value = (String) target;
			if (invalidValue.equals(value)) {
				errors.reject("invalid value '"+invalidValue+"'");
			}
		}
	}

}
