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

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.server.config.WebSocketConfigurationSupport;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;
import org.springframework.web.socket.support.TestWebSocketSession;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link WebSocketConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketMessageBrokerConfigurationTests {

	@Before
	public void setup() {
	}

	@Test
	public void webSocketHandler() throws Exception {

		AnnotationConfigApplicationContext cxt = new AnnotationConfigApplicationContext();
		cxt.register(TestWebSocketMessageBrokerConfiguration.class, SimpleBrokerConfigurer.class);
		cxt.refresh();

		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) cxt.getBean(HandlerMapping.class);
		Object actual = hm.getUrlMap().get("/e1");

		assertNotNull(actual);
		assertEquals(WebSocketHttpRequestHandler.class, actual.getClass());

		cxt.close();
	}

	@Test
	public void webSocketHandlerWithSockJS() throws Exception {

		AnnotationConfigApplicationContext cxt = new AnnotationConfigApplicationContext();
		cxt.register(TestWebSocketMessageBrokerConfiguration.class, SimpleBrokerConfigurer.class);
		cxt.refresh();

		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) cxt.getBean(HandlerMapping.class);
		Object actual = hm.getUrlMap().get("/e2/**");

		assertNotNull(actual);
		assertEquals(SockJsHttpRequestHandler.class, actual.getClass());

		cxt.close();
	}

	@Test
	public void annotationMethodMessageHandler() throws Exception {

		AnnotationConfigApplicationContext cxt = new AnnotationConfigApplicationContext();
		cxt.register(TestWebSocketMessageBrokerConfiguration.class, SimpleBrokerConfigurer.class);
		cxt.refresh();

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/app/foo");
		Message<byte[]> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		byte[] bytes = new StompMessageConverter().fromMessage(message);

		TestWebSocketSession session = new TestWebSocketSession();
		session.setAcceptedProtocol("v12.stomp");

		SubProtocolWebSocketHandler wsHandler = cxt.getBean(SubProtocolWebSocketHandler.class);
		wsHandler.handleMessage(session, new TextMessage(new String(bytes)));

		assertTrue(cxt.getBean(TestController.class).foo);

		cxt.close();
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

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/e1");
			registry.addEndpoint("/e2").withSockJS();
		}

		@Override
		public void configureMessageBroker(MessageBrokerConfigurer configurer) {
			configurer.setAnnotationMethodDestinationPrefixes("/app/");
			configurer.enableSimpleBroker("/topic");
		}
	}

	@Controller
	private static class TestController {

		private boolean foo;


		@MessageMapping(value="/app/foo")
		public void handleFoo() {
			this.foo = true;
		}
	}

}
