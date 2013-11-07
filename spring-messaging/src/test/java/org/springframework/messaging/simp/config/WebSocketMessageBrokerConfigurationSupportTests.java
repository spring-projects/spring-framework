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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
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
import org.springframework.messaging.support.converter.CompositeMessageConverter;
import org.springframework.messaging.support.converter.DefaultContentTypeResolver;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.support.TestWebSocketSession;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


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

		SubscribableChannel channel = this.cxtSimpleBroker.getBean("webSocketRequestChannel", SubscribableChannel.class);

		ArgumentCaptor<MessageHandler> captor = ArgumentCaptor.forClass(MessageHandler.class);
		verify(channel, times(3)).subscribe(captor.capture());

		List<MessageHandler> values = captor.getAllValues();
		assertEquals(3, values.size());

		assertTrue(values.contains(cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(values.contains(cxtSimpleBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void webSocketRequestChannelWithStompBroker() {
		SubscribableChannel channel = this.cxtStompBroker.getBean("webSocketRequestChannel", SubscribableChannel.class);

		ArgumentCaptor<MessageHandler> captor = ArgumentCaptor.forClass(MessageHandler.class);
		verify(channel, times(3)).subscribe(captor.capture());

		List<MessageHandler> values = captor.getAllValues();
		assertEquals(3, values.size());
		assertTrue(values.contains(cxtStompBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void webSocketRequestChannelSendMessage() throws Exception {

		SubscribableChannel channel = this.cxtSimpleBroker.getBean("webSocketRequestChannel", SubscribableChannel.class);
		SubProtocolWebSocketHandler webSocketHandler = this.cxtSimpleBroker.getBean(SubProtocolWebSocketHandler.class);

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.SEND).headers("destination:/foo").build();
		webSocketHandler.handleMessage(new TestWebSocketSession(), textMessage);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(channel).send(captor.capture());

		Message message = captor.getValue();
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
	}

	@Test
	public void webSocketResponseChannel() {
		SubscribableChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", SubscribableChannel.class);
		verify(channel).subscribe(any(SubProtocolWebSocketHandler.class));
		verifyNoMoreInteractions(channel);
	}

	@Test
	public void webSocketResponseChannelUsedByAnnotatedMethod() {

		SubscribableChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", SubscribableChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		when(channel.send(any(Message.class))).thenReturn(true);
		messageHandler.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(channel).send(captor.capture());
		message = captor.getValue();
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
		assertEquals("\"bar\"", new String((byte[]) message.getPayload()));
	}

	@Test
	public void webSocketResponseChannelUsedBySimpleBroker() {
		SubscribableChannel channel = this.cxtSimpleBroker.getBean("webSocketResponseChannel", SubscribableChannel.class);
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
		when(channel.send(any(Message.class))).thenReturn(true);
		broker.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(channel).send(captor.capture());
		message = captor.getValue();
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
		assertEquals("bar", new String((byte[]) message.getPayload()));
	}

	@Test
	public void brokerChannel() {
		SubscribableChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", SubscribableChannel.class);

		ArgumentCaptor<MessageHandler> captor = ArgumentCaptor.forClass(MessageHandler.class);
		verify(channel, times(2)).subscribe(captor.capture());

		List<MessageHandler> values = captor.getAllValues();
		assertEquals(2, values.size());
		assertTrue(values.contains(cxtSimpleBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void brokerChannelWithStompBroker() {
		SubscribableChannel channel = this.cxtStompBroker.getBean("brokerChannel", SubscribableChannel.class);

		ArgumentCaptor<MessageHandler> captor = ArgumentCaptor.forClass(MessageHandler.class);
		verify(channel, times(2)).subscribe(captor.capture());

		List<MessageHandler> values = captor.getAllValues();
		assertEquals(2, values.size());
		assertTrue(values.contains(cxtStompBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void brokerChannelUsedByAnnotatedMethod() {
		SubscribableChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", SubscribableChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		when(channel.send(any(Message.class))).thenReturn(true);
		messageHandler.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(channel).send(captor.capture());
		message = captor.getValue();
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/bar", headers.getDestination());
		assertEquals("\"bar\"", new String((byte[]) message.getPayload()));
	}

	@Test
	public void brokerChannelUsedByUserDestinationMessageHandler() {
		SubscribableChannel channel = this.cxtSimpleBroker.getBean("brokerChannel", SubscribableChannel.class);
		UserDestinationMessageHandler messageHandler = this.cxtSimpleBroker.getBean(UserDestinationMessageHandler.class);

		this.cxtSimpleBroker.getBean(UserSessionRegistry.class).registerSessionId("joe", "s1");

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination("/user/joe/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		when(channel.send(any(Message.class))).thenReturn(true);
		messageHandler.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(channel).send(captor.capture());
		message = captor.getValue();
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
		public SubscribableChannel webSocketRequestChannel() {
			return Mockito.mock(SubscribableChannel.class);
		}

		@Override
		@Bean
		public SubscribableChannel webSocketResponseChannel() {
			return Mockito.mock(SubscribableChannel.class);
		}

		@Override
		public SubscribableChannel brokerChannel() {
			return Mockito.mock(SubscribableChannel.class);
		}
	}

}
