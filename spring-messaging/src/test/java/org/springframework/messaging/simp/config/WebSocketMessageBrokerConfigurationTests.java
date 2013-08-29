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

package org.springframework.messaging.simp.config;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.AbstractWebSocketIntegrationTests;
import org.springframework.messaging.simp.JettyTestServer;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.config.WebSocketConfigurationSupport;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link WebSocketConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class WebSocketMessageBrokerConfigurationTests extends AbstractWebSocketIntegrationTests {

	@Parameters
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][] {
				{ new JettyTestServer(), new JettyWebSocketClient()} });
	};


	@Test
	public void sendMessage() throws Exception {

		AnnotationConfigWebApplicationContext cxt = new AnnotationConfigWebApplicationContext();
		cxt.register(TestWebSocketMessageBrokerConfiguration.class, SimpleBrokerConfigurer.class);
		cxt.register(getUpgradeStrategyConfigClass());

		this.server.init(cxt);
		this.server.start();

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/app/foo");
		Message<byte[]> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		byte[] bytes = new StompMessageConverter().fromMessage(message);
		final TextMessage webSocketMessage = new TextMessage(new String(bytes));

		WebSocketHandler clientHandler = new TextWebSocketHandlerAdapter() {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				session.sendMessage(webSocketMessage);
			}
		};

		TestController testController = cxt.getBean(TestController.class);

		this.webSocketClient.doHandshake(clientHandler, getWsBaseUrl() + "/ws");
		assertTrue(testController.latch.await(2, TimeUnit.SECONDS));

		testController.latch = new CountDownLatch(1);
		this.webSocketClient.doHandshake(clientHandler, getWsBaseUrl() + "/sockjs/websocket");
		assertTrue(testController.latch.await(2, TimeUnit.SECONDS));
	}


	@Configuration
	static class TestWebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

		@Override
		@Bean
		public SubscribableChannel webSocketRequestChannel() {
			return new ExecutorSubscribableChannel(); // synchronous
		}

		@Override
		@Bean
		public SubscribableChannel webSocketReplyChannel() {
			return new ExecutorSubscribableChannel(); // synchronous
		}

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}

	@Configuration
	static class SimpleBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

		@Autowired
		private HandshakeHandler handshakeHandler; // can't rely on classpath for server detection


		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {

			registry.addEndpoint("/ws")
				.setHandshakeHandler(this.handshakeHandler);

			registry.addEndpoint("/sockjs").withSockJS()
				.setTransportHandlerOverrides(new WebSocketTransportHandler(this.handshakeHandler));;
		}

		@Override
		public void configureMessageBroker(MessageBrokerConfigurer configurer) {
			configurer.setAnnotationMethodDestinationPrefixes("/app/");
			configurer.enableSimpleBroker("/topic");
		}
	}

	@Controller
	private static class TestController {

		private CountDownLatch latch = new CountDownLatch(1);

		@MessageMapping(value="/app/foo")
		public void handleFoo() {
			this.latch.countDown();
		}
	}

}
