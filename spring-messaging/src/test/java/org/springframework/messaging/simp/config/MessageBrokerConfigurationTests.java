/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link AbstractMessageBrokerConfiguration}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class MessageBrokerConfigurationTests {

	private AnnotationConfigApplicationContext simpleContext;

	private AnnotationConfigApplicationContext brokerRelayContext;

	private AnnotationConfigApplicationContext customChannelContext;


	@Before
	public void setupOnce() {

		this.simpleContext = new AnnotationConfigApplicationContext();
		this.simpleContext.register(SimpleConfig.class);
		this.simpleContext.refresh();

		this.brokerRelayContext = new AnnotationConfigApplicationContext();
		this.brokerRelayContext.register(BrokerRelayConfig.class);
		this.brokerRelayContext.refresh();

		this.customChannelContext = new AnnotationConfigApplicationContext();
		this.customChannelContext.register(CustomChannelConfig.class);
		this.customChannelContext.refresh();
	}


	@Test
	public void clientInboundChannel() {

		TestChannel channel = this.simpleContext.getBean("clientInboundChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(3, handlers.size());
		assertTrue(handlers.contains(simpleContext.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(handlers.contains(simpleContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(simpleContext.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void clientInboundChannelWithBrokerRelay() {
		TestChannel channel = this.brokerRelayContext.getBean("clientInboundChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(3, handlers.size());
		assertTrue(handlers.contains(brokerRelayContext.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(handlers.contains(brokerRelayContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(brokerRelayContext.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void clientInboundChannelCustomized() {
		AbstractSubscribableChannel channel = this.customChannelContext.getBean(
				"clientInboundChannel", AbstractSubscribableChannel.class);

		assertEquals(1, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.customChannelContext.getBean(
				"clientInboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(11, taskExecutor.getCorePoolSize());
		assertEquals(12, taskExecutor.getMaxPoolSize());
		assertEquals(13, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void clientOutboundChannelUsedByAnnotatedMethod() {
		TestChannel channel = this.simpleContext.getBean("clientOutboundChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.simpleContext.getBean(SimpAnnotationMethodMessageHandler.class);

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
		TestChannel channel = this.simpleContext.getBean("clientOutboundChannel", TestChannel.class);
		SimpleBrokerMessageHandler broker = this.simpleContext.getBean(SimpleBrokerMessageHandler.class);

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

		AbstractSubscribableChannel channel = this.customChannelContext.getBean(
				"clientOutboundChannel", AbstractSubscribableChannel.class);

		assertEquals(2, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.customChannelContext.getBean(
				"clientOutboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(21, taskExecutor.getCorePoolSize());
		assertEquals(22, taskExecutor.getMaxPoolSize());
		assertEquals(23, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void brokerChannel() {
		TestChannel channel = this.simpleContext.getBean("brokerChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(2, handlers.size());
		assertTrue(handlers.contains(simpleContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(simpleContext.getBean(SimpleBrokerMessageHandler.class)));
	}

	@Test
	public void brokerChannelWithBrokerRelay() {
		TestChannel channel = this.brokerRelayContext.getBean("brokerChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(2, handlers.size());
		assertTrue(handlers.contains(brokerRelayContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(brokerRelayContext.getBean(StompBrokerRelayMessageHandler.class)));
	}

	@Test
	public void brokerChannelUsedByAnnotatedMethod() {
		TestChannel channel = this.simpleContext.getBean("brokerChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.simpleContext.getBean(SimpAnnotationMethodMessageHandler.class);

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
		TestChannel channel = this.simpleContext.getBean("brokerChannel", TestChannel.class);
		UserDestinationMessageHandler messageHandler = this.simpleContext.getBean(UserDestinationMessageHandler.class);

		this.simpleContext.getBean(UserSessionRegistry.class).registerSessionId("joe", "s1");

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

		AbstractSubscribableChannel channel = this.customChannelContext.getBean(
				"brokerChannel", AbstractSubscribableChannel.class);

		assertEquals(3, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.customChannelContext.getBean(
				"brokerChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(31, taskExecutor.getCorePoolSize());
		assertEquals(32, taskExecutor.getMaxPoolSize());
		assertEquals(33, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void configureMessageConvertersDefault() {
		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {};
		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();

		List<MessageConverter> converters = compositeConverter.getConverters();
		assertThat(converters.size(), Matchers.is(3));
		assertThat(converters.get(0), Matchers.instanceOf(MappingJackson2MessageConverter.class));
		assertThat(converters.get(1), Matchers.instanceOf(StringMessageConverter.class));
		assertThat(converters.get(2), Matchers.instanceOf(ByteArrayMessageConverter.class));

		ContentTypeResolver resolver = ((MappingJackson2MessageConverter) converters.get(0)).getContentTypeResolver();
		assertEquals(MimeTypeUtils.APPLICATION_JSON, ((DefaultContentTypeResolver) resolver).getDefaultMimeType());
	}

	@Test
	public void configureMessageConvertersCustom() {
		final MessageConverter testConverter = Mockito.mock(MessageConverter.class);
		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {
			@Override
			protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
				messageConverters.add(testConverter);
				return false;
			}
		};

		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();
		assertThat(compositeConverter.getConverters().size(), Matchers.is(1));
		Iterator<MessageConverter> iterator = compositeConverter.getConverters().iterator();
		assertThat(iterator.next(), Matchers.is(testConverter));
	}

	@Test
	public void configureMessageConvertersCustomAndDefault() {

		final MessageConverter testConverter = Mockito.mock(MessageConverter.class);

		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {
			@Override
			protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
				messageConverters.add(testConverter);
				return true;
			}
		};
		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();

		assertThat(compositeConverter.getConverters().size(), Matchers.is(4));
		Iterator<MessageConverter> iterator = compositeConverter.getConverters().iterator();
		assertThat(iterator.next(), Matchers.is(testConverter));
		assertThat(iterator.next(), Matchers.instanceOf(MappingJackson2MessageConverter.class));
		assertThat(iterator.next(), Matchers.instanceOf(StringMessageConverter.class));
		assertThat(iterator.next(), Matchers.instanceOf(ByteArrayMessageConverter.class));
	}

	@Test
	public void simpValidatorDefault() {
		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {};
		config.setApplicationContext(new StaticApplicationContext());

		assertThat(config.simpValidator(), Matchers.notNullValue());
		assertThat(config.simpValidator(), Matchers.instanceOf(OptionalValidatorFactoryBean.class));
	}

	@Test
	public void simpValidatorCustom() {
		final Validator validator = Mockito.mock(Validator.class);
		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {
			@Override
			public Validator getValidator() {
				return validator;
			}
		};

		assertSame(validator, config.simpValidator());
	}

	@Test
	public void simpValidatorMvc() {
		StaticApplicationContext appCxt = new StaticApplicationContext();
		appCxt.registerSingleton("mvcValidator", TestValidator.class);
		AbstractMessageBrokerConfiguration config = new AbstractMessageBrokerConfiguration() {};
		config.setApplicationContext(appCxt);

		assertThat(config.simpValidator(), Matchers.notNullValue());
		assertThat(config.simpValidator(), Matchers.instanceOf(TestValidator.class));
	}

	@Test
	public void simpValidatorInjected() {
		SimpAnnotationMethodMessageHandler messageHandler =
				this.simpleContext.getBean(SimpAnnotationMethodMessageHandler.class);

		assertThat(messageHandler.getValidator(), Matchers.notNullValue(Validator.class));
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
	static class SimpleConfig extends AbstractMessageBrokerConfiguration {

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
		@Bean
		public AbstractSubscribableChannel clientOutboundChannel() {
			return new TestChannel();
		}

		@Override
		@Bean
		public AbstractSubscribableChannel brokerChannel() {
			return new TestChannel();
		}
	}

	@Configuration
	static class BrokerRelayConfig extends SimpleConfig {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableStompBrokerRelay("/topic", "/queue").setAutoStartup(true);
		}
	}

	@Configuration
	static class CustomChannelConfig extends AbstractMessageBrokerConfiguration {

		private ChannelInterceptor interceptor = new ChannelInterceptorAdapter() {};

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
			registry.configureBrokerChannel().setInterceptors(
					this.interceptor, this.interceptor, this.interceptor);
			registry.configureBrokerChannel().taskExecutor()
					.corePoolSize(31).maxPoolSize(32).keepAliveSeconds(33).queueCapacity(34);
		}
	}


	private static class TestChannel extends ExecutorSubscribableChannel {

		private final List<Message<?>> messages = new ArrayList<>();

		@Override
		public boolean sendInternal(Message<?> message, long timeout) {
			this.messages.add(message);
			return true;
		}
	}

	private static class TestValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}
	}

}
