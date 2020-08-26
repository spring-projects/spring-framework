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

package org.springframework.web.socket.messaging;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebSocketAnnotationMethodMessageHandler}.
 * @author Rossen Stoyanchev
 */
public class WebSocketAnnotationMethodMessageHandlerTests {

	private TestWebSocketAnnotationMethodMessageHandler messageHandler;

	private StaticApplicationContext applicationContext;


	@BeforeEach
	public void setUp() throws Exception {
		this.applicationContext = new StaticApplicationContext();
		this.applicationContext.registerSingleton("controller", TestController.class);
		this.applicationContext.registerSingleton("controllerAdvice", TestControllerAdvice.class);
		this.applicationContext.refresh();

		SubscribableChannel channel = Mockito.mock(SubscribableChannel.class);
		SimpMessageSendingOperations brokerTemplate = new SimpMessagingTemplate(channel);

		this.messageHandler = new TestWebSocketAnnotationMethodMessageHandler(brokerTemplate, channel, channel);
		this.messageHandler.setApplicationContext(this.applicationContext);
		this.messageHandler.afterPropertiesSet();
	}

	@Test
	public void globalException() throws Exception {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setSessionId("session1");
		headers.setSessionAttributes(new ConcurrentHashMap<>());
		headers.setDestination("/exception");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		this.messageHandler.handleMessage(message);

		TestControllerAdvice controllerAdvice = this.applicationContext.getBean(TestControllerAdvice.class);
		assertThat(controllerAdvice.isExceptionHandled()).isTrue();
	}


	@Controller
	private static class TestController {

		@MessageMapping("/exception")
		public void handleWithSimulatedException() {
			throw new IllegalStateException("simulated exception");
		}
	}

	@ControllerAdvice
	private static class TestControllerAdvice {

		private boolean exceptionHandled;


		public boolean isExceptionHandled() {
			return this.exceptionHandled;
		}

		@MessageExceptionHandler
		public void handleException(IllegalStateException ex) {
			this.exceptionHandled = true;
		}
	}

	private static class TestWebSocketAnnotationMethodMessageHandler extends WebSocketAnnotationMethodMessageHandler {

		public TestWebSocketAnnotationMethodMessageHandler(SimpMessageSendingOperations brokerTemplate,
				SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel) {

			super(clientInboundChannel, clientOutboundChannel, brokerTemplate);
		}
	}

}
