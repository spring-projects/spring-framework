/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.AbstractWebSocketIntegrationTests;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.socket.messaging.StompTextMessageBuilder.create;

/**
 * Integration tests with annotated message-handling methods.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class StompWebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {

	private static final long TIMEOUT = 10;


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] {TestMessageBrokerConfiguration.class, TestMessageBrokerConfigurer.class};
	}


	@ParameterizedWebSocketTest
	void sendMessageToController(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		TextMessage message = create(StompCommand.SEND).headers("destination:/app/simple").build();

		try (WebSocketSession session = doHandshake(new TestClientWebSocketHandler(0, message), "/ws").get()) {
			assertThat(session).isNotNull();
			SimpleController controller = this.wac.getBean(SimpleController.class);
			assertThat(controller.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
		}
	}

	@ParameterizedWebSocketTest
	void sendMessageToControllerAndReceiveReplyViaTopic(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE)
				.headers("id:subs1", "destination:/topic/increment").build();
		TextMessage m2 = create(StompCommand.SEND)
				.headers("destination:/app/increment").body("5").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(2, m0, m1, m2);

		try (WebSocketSession session = doHandshake(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
		}
	}

	@ParameterizedWebSocketTest  // SPR-10930
	void sendMessageToBrokerAndReceiveReplyViaTopic(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", "destination:/topic/foo").build();
		TextMessage m2 = create(StompCommand.SEND).headers("destination:/topic/foo").body("5").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(2, m0, m1, m2);

		try (WebSocketSession session = doHandshake(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

			String payload = clientHandler.actual.get(1).getPayload();
			assertThat(payload.startsWith("MESSAGE\n")).as("Expected STOMP MESSAGE, got " + payload).isTrue();
		}
	}

	@ParameterizedWebSocketTest  // SPR-11648
	void sendSubscribeToControllerAndReceiveReply(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		String destHeader = "destination:/app/number";
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", destHeader).build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(2, m0, m1);

		try (WebSocketSession session = doHandshake(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(1).getPayload();
			assertThat(payload.contains(destHeader)).as("Expected STOMP destination=/app/number, got " + payload).isTrue();
			assertThat(payload.contains("42")).as("Expected STOMP Payload=42, got " + payload).isTrue();
		}
	}

	@ParameterizedWebSocketTest
	void handleExceptionAndSendToUser(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		String destHeader = "destination:/user/queue/error";
		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", destHeader).build();
		TextMessage m2 = create(StompCommand.SEND).headers("destination:/app/exception").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(2, m0, m1, m2);

		try (WebSocketSession session = doHandshake(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(1).getPayload();
			assertThat(payload.startsWith("MESSAGE\n")).isTrue();
			assertThat(payload.contains("destination:/user/queue/error\n")).isTrue();
			assertThat(payload.endsWith("Got error: Bad input\0")).isTrue();
		}
	}

	@ParameterizedWebSocketTest
	void webSocketScope(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE)
				.headers("id:subs1", "destination:/topic/scopedBeanValue").build();
		TextMessage m2 = create(StompCommand.SEND)
				.headers("destination:/app/scopedBeanValue").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(2, m0, m1, m2);

		try (WebSocketSession session = doHandshake(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(1).getPayload();
			assertThat(payload.startsWith("MESSAGE\n")).isTrue();
			assertThat(payload.contains("destination:/topic/scopedBeanValue\n")).isTrue();
			assertThat(payload.endsWith("55\0")).isTrue();
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Controller
	private @interface IntegrationTestController {
	}


	@IntegrationTestController
	static class SimpleController {

		private CountDownLatch latch = new CountDownLatch(1);

		@MessageMapping("/simple")
		public void handle() {
			this.latch.countDown();
		}

		@MessageMapping("/exception")
		public void handleWithError() {
			throw new IllegalArgumentException("Bad input");
		}

		@MessageExceptionHandler
		@SendToUser("/queue/error")
		public String handleException(IllegalArgumentException ex) {
			return "Got error: " + ex.getMessage();
		}
	}


	@IntegrationTestController
	static class IncrementController {

		@MessageMapping("/increment")
		public int handle(int i) {
			return i + 1;
		}

		@SubscribeMapping("/number")
		public int number() {
			return 42;
		}
	}


	@IntegrationTestController
	static class ScopedBeanController {

		private final ScopedBean scopedBean;

		@Autowired
		public ScopedBeanController(ScopedBean scopedBean) {
			this.scopedBean = scopedBean;
		}

		@MessageMapping("/scopedBeanValue")
		public String getValue() {
			return this.scopedBean.getValue();
		}
	}


	interface ScopedBean {

		String getValue();
	}


	static class ScopedBeanImpl implements ScopedBean {

		private final String value;

		public ScopedBeanImpl(String value) {
			this.value = value;
		}

		@Override
		public String getValue() {
			return this.value;
		}
	}


	private static class TestClientWebSocketHandler extends TextWebSocketHandler {

		private final TextMessage[] messagesToSend;

		private final int expected;

		private final List<TextMessage> actual = new CopyOnWriteArrayList<>();

		private final CountDownLatch latch;

		public TestClientWebSocketHandler(int expectedNumberOfMessages, TextMessage... messagesToSend) {
			this.messagesToSend = messagesToSend;
			this.expected = expectedNumberOfMessages;
			this.latch = new CountDownLatch(this.expected);
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			for (TextMessage message : this.messagesToSend) {
				session.sendMessage(message);
			}
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			this.actual.add(message);
			this.latch.countDown();
		}
	}


	@Configuration
	@ComponentScan(
			basePackageClasses = StompWebSocketIntegrationTests.class,
			useDefaultFilters = false,
			includeFilters = @ComponentScan.Filter(IntegrationTestController.class))
	static class TestMessageBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

		@Autowired
		private HandshakeHandler handshakeHandler;  // can't rely on classpath for server detection

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/ws").setHandshakeHandler(this.handshakeHandler);
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry configurer) {
			configurer.setApplicationDestinationPrefixes("/app");
			configurer.enableSimpleBroker("/topic", "/queue");
		}

		@Bean
		@Scope(scopeName = "websocket", proxyMode = ScopedProxyMode.INTERFACES)
		public ScopedBean scopedBean() {
			return new ScopedBeanImpl("55");
		}
	}


	@Configuration
	static class TestMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

		@Override
		@Bean
		public AbstractSubscribableChannel clientInboundChannel() {
			return new ExecutorSubscribableChannel();  // synchronous
		}

		@Override
		@Bean
		public AbstractSubscribableChannel clientOutboundChannel() {
			return new ExecutorSubscribableChannel();  // synchronous
		}
	}

}
