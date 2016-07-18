/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

/**
 * Test fixture for {@link AbstractMessageBrokerConfiguration}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class MessageBrokerConfigurationTests {

	private ApplicationContext defaultContext = new AnnotationConfigApplicationContext(DefaultConfig.class);

	private ApplicationContext simpleBrokerContext = new AnnotationConfigApplicationContext(SimpleBrokerConfig.class);

	private ApplicationContext brokerRelayContext = new AnnotationConfigApplicationContext(BrokerRelayConfig.class);

	private ApplicationContext customContext = new AnnotationConfigApplicationContext(CustomConfig.class);


	@Test
	public void clientInboundChannel() {
		TestChannel channel = this.simpleBrokerContext.getBean("clientInboundChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(3, handlers.size());
		assertTrue(handlers.contains(simpleBrokerContext.getBean(SimpAnnotationMethodMessageHandler.class)));
		assertTrue(handlers.contains(simpleBrokerContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(simpleBrokerContext.getBean(SimpleBrokerMessageHandler.class)));
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
		AbstractSubscribableChannel channel = this.customContext.getBean(
				"clientInboundChannel", AbstractSubscribableChannel.class);

		assertEquals(3, channel.getInterceptors().size());

		CustomThreadPoolTaskExecutor taskExecutor = this.customContext.getBean(
				"clientInboundChannelExecutor", CustomThreadPoolTaskExecutor.class);

		assertEquals(11, taskExecutor.getCorePoolSize());
		assertEquals(12, taskExecutor.getMaxPoolSize());
		assertEquals(13, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void clientOutboundChannelUsedByAnnotatedMethod() {
		TestChannel channel = this.simpleBrokerContext.getBean("clientOutboundChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler = this.simpleBrokerContext.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSessionAttributes(new ConcurrentHashMap<>());
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/foo", headers.getDestination());
		assertEquals("bar", new String((byte[]) message.getPayload()));
	}

	@Test
	public void clientOutboundChannelUsedBySimpleBroker() {
		TestChannel channel = this.simpleBrokerContext.getBean("clientOutboundChannel", TestChannel.class);
		SimpleBrokerMessageHandler broker = this.simpleBrokerContext.getBean(SimpleBrokerMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		// subscribe
		broker.handleMessage(message);

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setDestination("/foo");
		message = MessageBuilder.createMessage("bar".getBytes(), headers.getMessageHeaders());

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
		AbstractSubscribableChannel channel = this.customContext.getBean(
				"clientOutboundChannel", AbstractSubscribableChannel.class);

		assertEquals(3, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.customContext.getBean(
				"clientOutboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(21, taskExecutor.getCorePoolSize());
		assertEquals(22, taskExecutor.getMaxPoolSize());
		assertEquals(23, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void brokerChannel() {
		TestChannel channel = this.simpleBrokerContext.getBean("brokerChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertEquals(2, handlers.size());
		assertTrue(handlers.contains(simpleBrokerContext.getBean(UserDestinationMessageHandler.class)));
		assertTrue(handlers.contains(simpleBrokerContext.getBean(SimpleBrokerMessageHandler.class)));

		assertNull(channel.getExecutor());
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
		TestChannel channel = this.simpleBrokerContext.getBean("brokerChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler =
				this.simpleBrokerContext.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setSessionAttributes(new ConcurrentHashMap<>());
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/bar", headers.getDestination());
		assertEquals("bar", new String((byte[]) message.getPayload()));
	}

	@Test
	public void brokerChannelCustomized() {
		AbstractSubscribableChannel channel = this.customContext.getBean(
				"brokerChannel", AbstractSubscribableChannel.class);

		assertEquals(4, channel.getInterceptors().size());

		ThreadPoolTaskExecutor taskExecutor = this.customContext.getBean(
				"brokerChannelExecutor", ThreadPoolTaskExecutor.class);

		assertEquals(31, taskExecutor.getCorePoolSize());
		assertEquals(32, taskExecutor.getMaxPoolSize());
		assertEquals(33, taskExecutor.getKeepAliveSeconds());
	}

	@Test
	public void configureMessageConvertersDefault() {
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig();
		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();

		List<MessageConverter> converters = compositeConverter.getConverters();
		assertThat(converters.size(), Matchers.is(3));
		assertThat(converters.get(0), Matchers.instanceOf(StringMessageConverter.class));
		assertThat(converters.get(1), Matchers.instanceOf(ByteArrayMessageConverter.class));
		assertThat(converters.get(2), Matchers.instanceOf(MappingJackson2MessageConverter.class));

		ContentTypeResolver resolver = ((MappingJackson2MessageConverter) converters.get(2)).getContentTypeResolver();
		assertEquals(MimeTypeUtils.APPLICATION_JSON, ((DefaultContentTypeResolver) resolver).getDefaultMimeType());
	}

	@Test
	public void threadPoolSizeDefault() {
		String name = "clientInboundChannelExecutor";
		ThreadPoolTaskExecutor executor = this.defaultContext.getBean(name, ThreadPoolTaskExecutor.class);
		assertEquals(Runtime.getRuntime().availableProcessors() * 2, executor.getCorePoolSize());
		// No way to verify queue capacity

		name = "clientOutboundChannelExecutor";
		executor = this.defaultContext.getBean(name, ThreadPoolTaskExecutor.class);
		assertEquals(Runtime.getRuntime().availableProcessors() * 2, executor.getCorePoolSize());

		name = "brokerChannelExecutor";
		executor = this.defaultContext.getBean(name, ThreadPoolTaskExecutor.class);
		assertEquals(0, executor.getCorePoolSize());
		assertEquals(1, executor.getMaxPoolSize());
	}

	@Test
	public void configureMessageConvertersCustom() {
		final MessageConverter testConverter = mock(MessageConverter.class);
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
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
		final MessageConverter testConverter = mock(MessageConverter.class);

		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
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
		assertThat(iterator.next(), Matchers.instanceOf(StringMessageConverter.class));
		assertThat(iterator.next(), Matchers.instanceOf(ByteArrayMessageConverter.class));
		assertThat(iterator.next(), Matchers.instanceOf(MappingJackson2MessageConverter.class));
	}

	@Test
	public void customArgumentAndReturnValueTypes() throws Exception {
		SimpAnnotationMethodMessageHandler handler = this.customContext.getBean(SimpAnnotationMethodMessageHandler.class);

		List<HandlerMethodArgumentResolver> customResolvers = handler.getCustomArgumentResolvers();
		assertEquals(1, customResolvers.size());
		assertTrue(handler.getArgumentResolvers().contains(customResolvers.get(0)));

		List<HandlerMethodReturnValueHandler> customHandlers = handler.getCustomReturnValueHandlers();
		assertEquals(1, customHandlers.size());
		assertTrue(handler.getReturnValueHandlers().contains(customHandlers.get(0)));
	}

	@Test
	public void simpValidatorDefault() {
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {};
		config.setApplicationContext(new StaticApplicationContext());

		assertThat(config.simpValidator(), Matchers.notNullValue());
		assertThat(config.simpValidator(), Matchers.instanceOf(OptionalValidatorFactoryBean.class));
	}

	@Test
	public void simpValidatorCustom() {
		final Validator validator = mock(Validator.class);
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
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
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {};
		config.setApplicationContext(appCxt);

		assertThat(config.simpValidator(), Matchers.notNullValue());
		assertThat(config.simpValidator(), Matchers.instanceOf(TestValidator.class));
	}

	@Test
	public void simpValidatorInjected() {
		SimpAnnotationMethodMessageHandler messageHandler =
				this.simpleBrokerContext.getBean(SimpAnnotationMethodMessageHandler.class);

		assertThat(messageHandler.getValidator(), Matchers.notNullValue(Validator.class));
	}

	@Test
	public void customPathMatcher() {
		SimpleBrokerMessageHandler broker = this.customContext.getBean(SimpleBrokerMessageHandler.class);
		DefaultSubscriptionRegistry registry = (DefaultSubscriptionRegistry) broker.getSubscriptionRegistry();
		assertEquals("a.a", registry.getPathMatcher().combine("a", "a"));

		SimpAnnotationMethodMessageHandler handler = this.customContext.getBean(SimpAnnotationMethodMessageHandler.class);
		assertEquals("a.a", handler.getPathMatcher().combine("a", "a"));

		DefaultUserDestinationResolver resolver = this.customContext.getBean(DefaultUserDestinationResolver.class);
		assertNotNull(resolver);
		assertEquals(false, new DirectFieldAccessor(resolver).getPropertyValue("keepLeadingSlash"));
	}

	@Test
	public void userBroadcasts() throws Exception {
		SimpUserRegistry userRegistry = this.brokerRelayContext.getBean(SimpUserRegistry.class);
		assertEquals(MultiServerUserRegistry.class, userRegistry.getClass());

		UserDestinationMessageHandler handler1 = this.brokerRelayContext.getBean(UserDestinationMessageHandler.class);
		assertEquals("/topic/unresolved-user-destination", handler1.getBroadcastDestination());

		UserRegistryMessageHandler handler2 = this.brokerRelayContext.getBean(UserRegistryMessageHandler.class);
		assertEquals("/topic/simp-user-registry", handler2.getBroadcastDestination());

		StompBrokerRelayMessageHandler relay = this.brokerRelayContext.getBean(StompBrokerRelayMessageHandler.class);
		assertNotNull(relay.getSystemSubscriptions());
		assertEquals(2, relay.getSystemSubscriptions().size());
		assertSame(handler1, relay.getSystemSubscriptions().get("/topic/unresolved-user-destination"));
		assertSame(handler2, relay.getSystemSubscriptions().get("/topic/simp-user-registry"));
	}

	@Test
	public void userBroadcastsDisabledWithSimpleBroker() throws Exception {
		SimpUserRegistry registry = this.simpleBrokerContext.getBean(SimpUserRegistry.class);
		assertNotNull(registry);
		assertNotEquals(MultiServerUserRegistry.class, registry.getClass());

		UserDestinationMessageHandler handler = this.simpleBrokerContext.getBean(UserDestinationMessageHandler.class);
		assertNull(handler.getBroadcastDestination());

		String name = "userRegistryMessageHandler";
		MessageHandler messageHandler = this.simpleBrokerContext.getBean(name, MessageHandler.class);
		assertNotEquals(UserRegistryMessageHandler.class, messageHandler.getClass());
	}


	@SuppressWarnings("unused")
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

	static class BaseTestMessageBrokerConfig extends AbstractMessageBrokerConfiguration {

		@Override
		protected SimpUserRegistry createLocalUserRegistry() {
			return mock(SimpUserRegistry.class);
		}
	}

	@SuppressWarnings("unused")
	@Configuration
	static class SimpleBrokerConfig extends BaseTestMessageBrokerConfig {

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
	static class BrokerRelayConfig extends SimpleBrokerConfig {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableStompBrokerRelay("/topic", "/queue").setAutoStartup(true)
					.setUserDestinationBroadcast("/topic/unresolved-user-destination")
					.setUserRegistryBroadcast("/topic/simp-user-registry");
		}
	}

	@Configuration
	static class DefaultConfig extends BaseTestMessageBrokerConfig {
	}

	@Configuration
	static class CustomConfig extends BaseTestMessageBrokerConfig {

		private ChannelInterceptor interceptor = new ChannelInterceptor() {};

		@Override
		protected void configureClientInboundChannel(ChannelRegistration registration) {
			registration.setInterceptors(this.interceptor);
			registration.taskExecutor(new CustomThreadPoolTaskExecutor())
					.corePoolSize(11).maxPoolSize(12).keepAliveSeconds(13).queueCapacity(14);
		}

		@Override
		protected void configureClientOutboundChannel(ChannelRegistration registration) {
			registration.setInterceptors(this.interceptor, this.interceptor);
			registration.taskExecutor().corePoolSize(21).maxPoolSize(22).keepAliveSeconds(23).queueCapacity(24);
		}

		@Override
		protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(mock(HandlerMethodArgumentResolver.class));
		}

		@Override
		protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(mock(HandlerMethodReturnValueHandler.class));
		}

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.configureBrokerChannel().setInterceptors(this.interceptor, this.interceptor, this.interceptor);
			registry.configureBrokerChannel().taskExecutor().corePoolSize(31).maxPoolSize(32).keepAliveSeconds(33).queueCapacity(34);
			registry.setPathMatcher(new AntPathMatcher(".")).enableSimpleBroker("/topic", "/queue");
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

	@SuppressWarnings("serial")
	private static class CustomThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
	}

}
