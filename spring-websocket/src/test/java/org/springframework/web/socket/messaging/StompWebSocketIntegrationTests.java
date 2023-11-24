/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.AbstractWebSocketIntegrationTests;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketTestServer;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
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
 * @author Sebastien Deleuze
 */
class StompWebSocketIntegrationTests extends AbstractWebSocketIntegrationTests {

	private static final long TIMEOUT = 10;


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] {TestMessageBrokerConfigurer.class};
	}


	@ParameterizedWebSocketTest
	void sendMessageToController(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		TextMessage message = create(StompCommand.SEND).headers("destination:/app/simple").build();

		try (WebSocketSession session = execute(new TestClientWebSocketHandler(0, message), "/ws").get()) {
			assertThat(session).isNotNull();
			SimpleController controller = this.wac.getBean(SimpleController.class);
			assertThat(controller.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
		}
	}

	@ParameterizedWebSocketTest
	void sendMessageToControllerAndReceiveReplyViaTopic(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE)
				.headers("id:subs1", "destination:/topic/increment").build();
		TextMessage m2 = create(StompCommand.SEND)
				.headers("destination:/app/increment").body("5").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, m0, m1, m2);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
		}
	}

	@ParameterizedWebSocketTest  // SPR-10930
	void sendMessageToBrokerAndReceiveReplyViaTopicWithSelectorHeader(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		String destination = "destination:/topic/foo";
		String selector = "selector:headers.foo == 'bar'";

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", destination, selector).build();
		TextMessage m2 = create(StompCommand.SEND).headers(destination, "foo:bar").body("5").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, m0, m1, m2);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

			String payload = clientHandler.actual.get(0).getPayload();
			assertThat(payload).as("Expected STOMP MESSAGE, got " + payload).startsWith("MESSAGE\n");
		}
	}

	@ParameterizedWebSocketTest // gh-21798
	void sendMessageToBrokerAndReceiveInOrder(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		String destination = "destination:/topic/foo";

		List<TextMessage> messages = new ArrayList<>();
		messages.add(create(StompCommand.CONNECT).headers("accept-version:1.1").build());
		messages.add(create(StompCommand.SUBSCRIBE).headers("id:subs1", destination).build());

		int count = 1000;
		for (int i = 0; i < count; i++) {
			messages.add(create(StompCommand.SEND).headers(destination).body(String.valueOf(i)).build());
		}

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(count, messages);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

			for (int i = 0; i < count; i++) {
				TextMessage message = clientHandler.actual.get(i);
				ByteBuffer buffer = ByteBuffer.wrap(message.asBytes());
				byte[] bytes = new StompDecoder().decode(buffer).get(0).getPayload();
				assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(String.valueOf(i));
			}
		}
	}

	@ParameterizedWebSocketTest // gh-21798
	void sendMessageToUserAndReceiveInOrder(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		UserFilter userFilter = new UserFilter(() -> "joe");
		super.setup(server, userFilter, webSocketClient, testInfo);

		List<TextMessage> messages = new ArrayList<>();
		messages.add(create(StompCommand.CONNECT).headers("accept-version:1.1").build());
		messages.add(create(StompCommand.SUBSCRIBE).headers("id:subs1", "destination:/user/queue/foo").build());

		int count = 1000;
		for (int i = 0; i < count; i++) {
			String dest = "destination:/user/joe/queue/foo";
			messages.add(create(StompCommand.SEND).headers(dest).body(String.valueOf(i)).build());
		}

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(count, messages);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

			for (int i = 0; i < count; i++) {
				TextMessage message = clientHandler.actual.get(i);
				ByteBuffer buffer = ByteBuffer.wrap(message.asBytes());
				byte[] bytes = new StompDecoder().decode(buffer).get(0).getPayload();
				assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(String.valueOf(i));
			}
		}
	}

	@ParameterizedWebSocketTest  // SPR-11648
	void sendSubscribeToControllerAndReceiveReply(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		String destHeader = "destination:/app/number";
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", destHeader).build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, m0, m1);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(0).getPayload();
			assertThat(payload).as("Expected STOMP destination=/app/number, got " + payload).contains(destHeader);
			assertThat(payload).as("Expected STOMP Payload=42, got " + payload).contains("42");
		}
	}

	@ParameterizedWebSocketTest
	void handleExceptionAndSendToUser(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		String destHeader = "destination:/user/queue/error";
		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE).headers("id:subs1", destHeader).build();
		TextMessage m2 = create(StompCommand.SEND).headers("destination:/app/exception").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, m0, m1, m2);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(0).getPayload();
			assertThat(payload).startsWith("MESSAGE\n");
			assertThat(payload).contains("destination:/user/queue/error\n");
			assertThat(payload).endsWith("Got error: Bad input\0");
		}
	}

	@ParameterizedWebSocketTest
	void webSocketScope(
			WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {

		super.setup(server, webSocketClient, testInfo);

		TextMessage m0 = create(StompCommand.CONNECT).headers("accept-version:1.1").build();
		TextMessage m1 = create(StompCommand.SUBSCRIBE)
				.headers("id:subs1", "destination:/topic/scopedBeanValue").build();
		TextMessage m2 = create(StompCommand.SEND)
				.headers("destination:/app/scopedBeanValue").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, m0, m1, m2);

		try (WebSocketSession session = execute(clientHandler, "/ws").get()) {
			assertThat(session).isNotNull();
			assertThat(clientHandler.latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
			String payload = clientHandler.actual.get(0).getPayload();
			assertThat(payload).startsWith("MESSAGE\n");
			assertThat(payload).contains("destination:/topic/scopedBeanValue\n");
			assertThat(payload).endsWith("55\0");
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


	private static class UserFilter implements Filter {

		private final Principal user;

		private UserFilter(Principal user) {
			this.user = user;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			request = new HttpServletRequestWrapper((HttpServletRequest) request) {
				@Override
				public Principal getUserPrincipal() {
					return user;
				}
			};

			chain.doFilter(request, response);
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

		private final List<TextMessage> messagesToSend;

		private final List<TextMessage> actual = new CopyOnWriteArrayList<>();

		private final CountDownLatch latch;

		TestClientWebSocketHandler(int expectedNumberOfMessages, TextMessage... messagesToSend) {
			this(expectedNumberOfMessages, Arrays.asList(messagesToSend));
		}

		TestClientWebSocketHandler(int expectedNumberOfMessages, List<TextMessage> messagesToSend) {
			this.messagesToSend = messagesToSend;
			this.latch = new CountDownLatch(expectedNumberOfMessages);
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			session.sendMessage(this.messagesToSend.get(0));
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			if (message.getPayload().startsWith("CONNECTED")) {
				for (int i = 1; i < this.messagesToSend.size(); i++) {
					session.sendMessage(this.messagesToSend.get(i));
				}
			}
			else {
				this.actual.add(message);
				this.latch.countDown();
			}
		}
	}


	@ComponentScan(
			basePackageClasses = StompWebSocketIntegrationTests.class,
			useDefaultFilters = false,
			includeFilters = @ComponentScan.Filter(IntegrationTestController.class))
	@EnableWebSocketMessageBroker
	static class TestMessageBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

		@Autowired
		private HandshakeHandler handshakeHandler;  // can't rely on classpath for server detection

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.setPreserveReceiveOrder(true);
			registry.addEndpoint("/ws").setHandshakeHandler(this.handshakeHandler);
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry configurer) {
			configurer.setApplicationDestinationPrefixes("/app");
			configurer.setPreservePublishOrder(true);
			configurer.enableSimpleBroker("/topic", "/queue").setSelectorHeaderName("selector");
			configurer.configureBrokerChannel().taskExecutor();
		}

		@Bean
		@Scope(scopeName = "websocket", proxyMode = ScopedProxyMode.INTERFACES)
		public ScopedBean scopedBean() {
			return new ScopedBeanImpl("55");
		}
	}

}
