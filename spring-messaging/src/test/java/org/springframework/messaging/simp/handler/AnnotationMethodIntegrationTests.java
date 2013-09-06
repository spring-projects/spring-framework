/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.messaging.simp.config.MessageBrokerConfigurer;
import org.springframework.messaging.simp.config.StompEndpointRegistry;
import org.springframework.messaging.simp.config.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.AbstractWebSocketIntegrationTests;
import org.springframework.web.socket.JettyWebSocketTestServer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.TomcatWebSocketTestServer;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.client.endpoint.StandardWebSocketClient;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.server.HandshakeHandler;

import static org.junit.Assert.*;
import static org.springframework.messaging.simp.stomp.StompTextMessageBuilder.*;


/**
 * Integration tests with annotated message-handling methods.
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class AnnotationMethodIntegrationTests extends AbstractWebSocketIntegrationTests {

	@Parameters
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][] {
				{new JettyWebSocketTestServer(), new JettyWebSocketClient()},
				{new TomcatWebSocketTestServer(), new StandardWebSocketClient()}
		});
	};


	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { TestMessageBrokerConfiguration.class, TestMessageBrokerConfigurer.class };
	}


	@Test
	public void simpleController() throws Exception {

		TextMessage message = create(StompCommand.SEND).headers("destination:/app/simple").build();
		WebSocketSession session = doHandshake(new TestClientWebSocketHandler(0, message), "/ws").get();

		SimpleController controller = this.wac.getBean(SimpleController.class);
		try {
			assertTrue(controller.latch.await(2, TimeUnit.SECONDS));
		}
		finally {
			session.close();
		}
	}

	@Test
	public void incrementController() throws Exception {

		TextMessage message1 = create(StompCommand.SUBSCRIBE).headers(
				"id:subs1", "destination:/topic/increment").body("5").build();

		TextMessage message2 = create(StompCommand.SEND).headers(
				"destination:/app/topic/increment").body("5").build();

		TestClientWebSocketHandler clientHandler = new TestClientWebSocketHandler(1, message1, message2);
		WebSocketSession session = doHandshake(clientHandler, "/ws").get();

		try {
			assertTrue(clientHandler.latch.await(2, TimeUnit.SECONDS));
		}
		finally {
			session.close();
		}
	}


	@IntegrationTestController
	static class SimpleController {

		private CountDownLatch latch = new CountDownLatch(1);

		@MessageMapping(value="/simple")
		public void handle() {
			this.latch.countDown();
		}
	}

	@IntegrationTestController
	static class IncrementController {

		@MessageMapping(value="/topic/increment")
		public int handle(int i) {
			return i + 1;
		}
	}


	private static class TestClientWebSocketHandler extends TextWebSocketHandlerAdapter {

		private final TextMessage[] messagesToSend;

		private final int expected;

		private final List<TextMessage> actual = new CopyOnWriteArrayList<TextMessage>();

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
	@ComponentScan(basePackageClasses=AnnotationMethodIntegrationTests.class,
			useDefaultFilters=false,
			includeFilters=@ComponentScan.Filter(IntegrationTestController.class))
	static class TestMessageBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

		@Autowired
		private HandshakeHandler handshakeHandler; // can't rely on classpath for server detection

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/ws").setHandshakeHandler(this.handshakeHandler);
		}

		@Override
		public void configureMessageBroker(MessageBrokerConfigurer configurer) {
			configurer.setAnnotationMethodDestinationPrefixes("/app");
			configurer.enableSimpleBroker("/topic", "/queue");
		}
	}

	@Configuration
	static class TestMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

		@Override
		@Bean
		public SubscribableChannel webSocketRequestChannel() {
			return new ExecutorSubscribableChannel(); // synchronous
		}

		@Override
		@Bean
		public SubscribableChannel webSocketResponseChannel() {
			return new ExecutorSubscribableChannel(); // synchronous
		}
	}

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Controller
	private @interface IntegrationTestController {
	}

}
