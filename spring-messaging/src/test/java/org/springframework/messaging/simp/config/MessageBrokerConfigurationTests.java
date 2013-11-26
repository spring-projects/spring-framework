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
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.handler.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.handler.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.handler.UserDestinationMessageHandler;
import org.springframework.messaging.simp.handler.UserSessionRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.AbstractSubscribableChannel;
import org.springframework.messaging.support.channel.ChannelInterceptor;
import org.springframework.messaging.support.channel.ChannelInterceptorAdapter;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.messaging.support.converter.CompositeMessageConverter;
import org.springframework.messaging.support.converter.DefaultContentTypeResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link AbstractMessageBrokerConfiguration}.
 *
 * @author Rossen Stoyanchev
 */
public class MessageBrokerConfigurationTests {

	private AnnotationConfigApplicationContext cxtSimpleBroker;

	private AnnotationConfigApplicationContext cxtStompBroker;

	private AnnotationConfigApplicationContext cxtCustomizedChannelConfig;


	@Before
	public void setupOnce() {

		this.cxtSimpleBroker = new AnnotationConfigApplicationContext();
		this.cxtSimpleBroker.register(TestMessageBrokerConfiguration.class);
		this.cxtSimpleBroker.refresh();

		this.cxtStompBroker = new AnnotationConfigApplicationContext();
		this.cxtStompBroker.register(TestStompMessageBrokerConfig.class);
		this.cxtStompBroker.refresh();

		this.cxtCustomizedChannelConfig = new AnnotationConfigApplicationContext();
		this.cxtCustomizedChannelConfig.register(CustomizedChannelConfig.class);
		this.cxtCustomizedChannelConfig.refresh();
	}


	@Test
	public void clientInboundChannel() {

		TestChannel channel = this.cxtSimpleBroker.getBean("clientInboundChannel", TestChannel.class);
		List<MessageHandler> handlers = channel.handlers;

		assertEquals(3, handlers.size());
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(cxtSimpleBroker.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void clientInboundChannelWithStompBroker() {
		TestChannel channel = this.cxtStompBroker.getBean("clientInboundChannel", TestChannel.class);
		List<MessageHandler> values = channel.handlers;

		assertEquals(3, values.size());
		assertTrue(values.contains(cxtStompBroker.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(UserDestinationMessageHandler.class)));
		assertTrue(values.contains(cxtStompBroker.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void clientInboundChannelCustomized() {

		AbstractSubscribableChannel channel = this.cxtCustomizedChannelConfig.getBean(
				"clientInboundChannel", AbstractSubscribableChannel.class);

		assertEquals(1, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.cxtCustomizedChannelConfig.getBean(
				"clientInboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(11, taskExecutor.getCorePoolSize());
		assertEquals(12, taskExecutor.getMaxPoolSize());
		assertEquals(13, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void clientOutboundChannelUsedByAnnotatedMethod() {

		TestChannel channel = this.cxtSimpleBroker.getBean("clientOutboundChannel", TestChannel.class);
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
	public void clientOutboundChannelUsedBySimpleBroker() {
		TestChannel channel = this.cxtSimpleBroker.getBean("clientOutboundChannel", TestChannel.class);
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
	public void clientOutboundChannelCustomized() {

		AbstractSubscribableChannel channel = this.cxtCustomizedChannelConfig.getBean(
				"clientOutboundChannel", AbstractSubscribableChannel.class);

		assertEquals(2, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.cxtCustomizedChannelConfig.getBean(
				"clientOutboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(21, taskExecutor.getCorePoolSize());
		assertEquals(22, taskExecutor.getMaxPoolSize());
		assertEquals(23, taskExecutor.getKeepAliveSeconds());
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
	public void brokerChannelCustomized() {

		AbstractSubscribableChannel channel = this.cxtCustomizedChannelConfig.getBean(
				"brokerChannel", AbstractSubscribableChannel.class);

		assertEquals(3, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.cxtCustomizedChannelConfig.getBean(
				"brokerChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(31, taskExecutor.getCorePoolSize());
		assertEquals(32, taskExecutor.getMaxPoolSize());
		assertEquals(33, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void messageConverter() {
		CompositeMessageConverter messageConverter = this.cxtStompBroker.getBean(
				"brokerMessageConverter", CompositeMessageConverter.class);

		DefaultContentTypeResolver resolver = (DefaultContentTypeResolver) messageConverter.getContentTypeResolver();
		assertEquals(MimeTypeUtils.APPLICATION_JSON, resolver.getDefaultMimeType());
	}


	@Controller
	static class TestController {

		@SubscribeMapping("/foo")
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
	static class TestMessageBrokerConfiguration extends AbstractMessageBrokerConfiguration {

		@Bean
		public TestController subscriptionController() {
			return new TestController();
		}


		@Override
		@Bean
		public AbstractSubscribableChannel clientInboundChannel() {
			return new TestChannel();
		}

		@Override
		protected void configureClientInboundChannel(ChannelRegistration registration) {
		}

		@Override
		@Bean
		public AbstractSubscribableChannel clientOutboundChannel() {
			return new TestChannel();
		}

		@Override
		protected void configureClientOutboundChannel(ChannelRegistration registration) {
		}

		@Override
		@Bean
		public AbstractSubscribableChannel brokerChannel() {
			return new TestChannel();
		}

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
		}

	}

	@Configuration
	static class TestStompMessageBrokerConfig extends TestMessageBrokerConfiguration {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableStompBrokerRelay("/topic", "/queue").setAutoStartup(true);
		}
	}

	@Configuration
	static class CustomizedChannelConfig extends AbstractMessageBrokerConfiguration {

		private ChannelInterceptor interceptor = new ChannelInterceptorAdapter();


		@Override
		protected void configureClientInboundChannel(ChannelRegistration registration) {
			registration.setInterceptors(this.interceptor);
			registration.taskExecutor().corePoolSize(11).maxPoolSize(12).keepAliveSeconds(13).queueCapacity(14);
		}

		@Override
		protected void configureClientOutboundChannel(ChannelRegistration registration) {
			registration.setInterceptors(this.interceptor, this.interceptor);
			registration.taskExecutor().corePoolSize(21).maxPoolSize(22).keepAliveSeconds(23).queueCapacity(24);
		}

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.configureBrokerChannel().setInterceptors(this.interceptor, this.interceptor, this.interceptor);
			registry.configureBrokerChannel().taskExecutor()
					.corePoolSize(31).maxPoolSize(32).keepAliveSeconds(33).queueCapacity(34);
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
