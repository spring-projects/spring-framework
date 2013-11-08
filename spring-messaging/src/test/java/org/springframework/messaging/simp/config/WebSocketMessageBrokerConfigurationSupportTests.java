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
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SubscribeEvent;
import org.springframework.messaging.simp.handler.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.handler.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.handler.UserDestinationMessageHandler;
import org.springframework.messaging.simp.handler.UserSessionRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompTextMessageBuilder;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.AbstractSubscribableChannel;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.messaging.support.converter.CompositeMessageConverter;
import org.springframework.messaging.support.converter.DefaultContentTypeResolver;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.support.TestWebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link WebSocketMessageBrokerConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketMessageBrokerConfigurationSupportTests {

	private AnnotationConfigApplicationContext cxtSimpleBroker;

	private AnnotationConfigApplicationContext cxtStompBroker;


	@Before
	public void setupOnce() {

		this.cxtSimpleBroker = new AnnotationConfigApplicationContext();
		this.cxtSimpleBroker.register(TestWebSocketMessageBrokerConfiguration.class, TestSimpleMessageBrokerConfig.class);
		this.cxtSimpleBroker.refresh();

		this.cxtStompBroker = new AnnotationConfigApplicationContext();
		this.cxtStompBroker.register(TestWebSocketMessageBrokerConfiguration.class, TestStompMessageBrokerConfig.class);
		this.cxtStompBroker.refresh();
	}

	@Test
	public void handlerMapping() {

		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.cxtSimpleBroker.getBean(HandlerMapping.class);
		assertEquals(1, hm.getOrder());

		Map<String, Object> handlerMap = hm.getHandlerMap();
		assertEquals(1, handlerMap.size());
		assertNotNull(handlerMap.get("/simpleBroker"));
	}

	@Test
	public void webSocketRequestChannel() {

		TestChannel channel = this.cxtSimpleBroker.getBean("webSocketRequestChannel", TestChannel.class);
		List<MessageHandler> handlers = channel.handlers;

		assertEquals(3, handlers.size());
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void webSocketRequestChannelWithStompBroker() {
		TestChannel channel = this.cxtStompBroker.getBean("webSocketRequestChannel", TestChannel.class);
		List<MessageHandler> values = channel.handlers;

		assertEquals(3, values.size());
		assertTrue(values.contains(cxtStompBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void webSocketRequestChannelSendMessage() throws Exception {

		TestChannel channel = this.cxtSimpleBroker.getBean("webSocketRequestChannel", TestChannel.class);
		SubProtocolWebSocketHandler webSocketHandler = this.cxtSimpleBroker.getBean(SubProtocolWebSocketHandler.class);

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.SEND).headers("destination:/foo").build();
		webSocketHandler.handleMessage(new TestWebSocketSession(), textMessage);

		Message<?> message = channel.messages.get(0);
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
	}

	@Test
	public void webSocketResponseChannel() {
		TestChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", TestChannel.class);
		List<MessageHandler> values = channel.handlers;

		assertEquals(1, values.size());
		assertTrue(values.get(0) instanceof SubProtocolWebSocketHandler);
	}

	@Test
	public void webSocketResponseChannelUsedByAnnotatedMethod() {

		TestChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
		assertEquals("\"bar\"", new String((byte[]) message.getPayload()));
	}

	@Test
	public void webSocketResponseChannelUsedBySimpleBroker() {
		TestChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", TestChannel.class);
		SimpleBrokerMessageHandler broker = this.cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		// subscribe
		broker.handleMessage(message);

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setDestination("/foo");
		message = MessageBuilder.withPayload("bar".getBytes()).setHeaders(headers).build();

		// message
		broker.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
		assertEquals("bar", new String((byte[]) message.getPayload()));
	}

	@Test
	public void brokerChannel() {
		TestChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", TestChannel.class);
		List<MessageHandler> handlers = channel.handlers;

		assertEquals(2, handlers.size());
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void brokerChannelWithStompBroker() {
		TestChannel channel = this.cxtStompBroker.getBean("brokerChannel", TestChannel.class);
		List<MessageHandler> handlers = channel.handlers;

		assertEquals(2, handlers.size());
		assertTrue(handlers.contains(cxtStompBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(cxtStompBroker.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void brokerChannelUsedByAnnotatedMethod() {
		TestChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/bar", headers.getDestination());
		assertEquals("\"bar\"", new String((byte[]) message.getPayload()));
	}

	@Test
	public void brokerChannelUsedByUserDestinationMessageHandler() {
		TestChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", TestChannel.class);
		UserDestinationMessageHandler messageHandler = this.cxtSimpleBroker.getBean(UserDestinationMessageHandler.class);

		this.cxtSimpleBroker.getBean(UserSessionRegistry.class).registerSessionId("joe", "s1");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/user/joe/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo-users1", headers.getDestination());
	}

	@Test
	public void messageConverter() {
		CompositeMessageConverter messageConverter = this.cxtStompBroker.getBean(
				"simpMessageConverter", CompositeMessageConverter.class);

		DefaultContentTypeResolver resolver = (DefaultContentTypeResolver) messageConverter.getContentTypeResolver();
		assertEquals(MimeTypeUtils.APPLICATION_JSON, resolver.getDefaultMimeType());
	}


	@Controller
	static class TestController {

		@SubscribeEvent("/foo")
		public String handleSubscribe() {
			return "bar";
		}

		@MessageMapping("/foo")
		@SendTo("/bar")
		public String handleMessage() {
			return "bar";
		}
	}

	@Configuration
	static class TestSimpleMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/simpleBroker");
		}

		@Override
		public void configureMessageBroker(MessageBrokerConfigurer configurer) {
			// SimpleBroker used by default
		}

		@Bean
		public TestController subscriptionController() {
			return new TestController();
		}
	}

	@Configuration
	static class TestStompMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/stompBrokerRelay");
		}

		@Override
		public void configureMessageBroker(MessageBrokerConfigurer configurer) {
			configurer.enableStompBrokerRelay("/topic", "/queue").setAutoStartup(false);
		}
	}

	@Configuration
	static class TestWebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

		@Override
		@Bean
		public AbstractSubscribableChannel webSocketRequestChannel() {
			return new TestChannel();
		}

		@Override
		@Bean
		public AbstractSubscribableChannel webSocketResponseChannel() {
			return new TestChannel();
		}

		@Override
		public AbstractSubscribableChannel brokerChannel() {
			return new TestChannel();
		}
	}

	private static class TestChannel extends ExecutorSubscribableChannel {

		private final List<MessageHandler> handlers = new ArrayList<>();

		private final List<Message<?>> messages = new ArrayList<>();


		@Override
		public boolean subscribeInternal(MessageHandler handler) {
			this.handlers.add(handler);
			return super.subscribeInternal(handler);
		}

		@Override
		public boolean sendInternal(Message<?> message, long timeout) {
			this.messages.add(message);
			return true;
		}
	}

}
