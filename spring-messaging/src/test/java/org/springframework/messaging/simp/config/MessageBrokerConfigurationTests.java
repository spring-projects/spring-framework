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

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.KotlinSerializationJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.tcp.ReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link AbstractMessageBrokerConfiguration}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class MessageBrokerConfigurationTests {

	@Test
	public void clientInboundChannel() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		TestChannel channel = context.getBean("clientInboundChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertThat(handlers).hasSize(3);
		assertThat(handlers.contains(context.getBean(SimpAnnotationMethodMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(UserDestinationMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(SimpleBrokerMessageHandler.class))).isTrue();
	}

	@Test
	public void clientInboundChannelWithBrokerRelay() {
		ApplicationContext context = loadConfig(BrokerRelayConfig.class);

		TestChannel channel = context.getBean("clientInboundChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertThat(handlers).hasSize(3);
		assertThat(handlers.contains(context.getBean(SimpAnnotationMethodMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(UserDestinationMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(StompBrokerRelayMessageHandler.class))).isTrue();
	}

	@Test
	public void clientInboundChannelCustomized() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		AbstractSubscribableChannel channel = context.getBean(
				"clientInboundChannel", AbstractSubscribableChannel.class);
		assertThat(channel.getInterceptors()).hasSize(3);

		CustomThreadPoolTaskExecutor taskExecutor = context.getBean(
				"clientInboundChannelExecutor", CustomThreadPoolTaskExecutor.class);
		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(11);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(12);
		assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(13);
	}

	@Test
	public void clientOutboundChannelUsedByAnnotatedMethod() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		TestChannel channel = context.getBean("clientOutboundChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler =
				context.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSessionAttributes(new ConcurrentHashMap<>());
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headers.getDestination()).isEqualTo("/foo");
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("bar");
	}

	@Test
	public void clientOutboundChannelUsedBySimpleBroker() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		TestChannel outboundChannel = context.getBean("clientOutboundChannel", TestChannel.class);
		SimpleBrokerMessageHandler broker = context.getBean(SimpleBrokerMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		// subscribe
		broker.handleMessage(createConnectMessage("sess1", new long[] {0,0}));
		broker.handleMessage(message);

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setDestination("/foo");
		message = MessageBuilder.createMessage("bar".getBytes(), headers.getMessageHeaders());

		// message
		broker.handleMessage(message);

		message = outboundChannel.messages.get(1);
		headers = StompHeaderAccessor.wrap(message);

		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headers.getDestination()).isEqualTo("/foo");
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("bar");
	}

	@Test
	public void clientOutboundChannelCustomized() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		AbstractSubscribableChannel channel = context.getBean(
				"clientOutboundChannel", AbstractSubscribableChannel.class);

		assertThat(channel.getInterceptors()).hasSize(4);

		ThreadPoolTaskExecutor taskExecutor = context.getBean(
				"clientOutboundChannelExecutor", ThreadPoolTaskExecutor.class);

		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(21);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(22);
		assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(23);

		SimpleBrokerMessageHandler broker =
				context.getBean("simpleBrokerMessageHandler", SimpleBrokerMessageHandler.class);
		assertThat(broker.isPreservePublishOrder()).isTrue();
	}

	@Test
	public void brokerChannel() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		TestChannel channel = context.getBean("brokerChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertThat(handlers).hasSize(2);
		assertThat(handlers.contains(context.getBean(UserDestinationMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(SimpleBrokerMessageHandler.class))).isTrue();

		assertThat((Object) channel.getExecutor()).isNull();
	}

	@Test
	public void brokerChannelWithBrokerRelay() {
		ApplicationContext context = loadConfig(BrokerRelayConfig.class);

		TestChannel channel = context.getBean("brokerChannel", TestChannel.class);
		Set<MessageHandler> handlers = channel.getSubscribers();

		assertThat(handlers).hasSize(2);
		assertThat(handlers.contains(context.getBean(UserDestinationMessageHandler.class))).isTrue();
		assertThat(handlers.contains(context.getBean(StompBrokerRelayMessageHandler.class))).isTrue();
	}

	@Test
	public void brokerChannelUsedByAnnotatedMethod() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		TestChannel channel = context.getBean("brokerChannel", TestChannel.class);
		SimpAnnotationMethodMessageHandler messageHandler =
				context.getBean(SimpAnnotationMethodMessageHandler.class);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setSessionAttributes(new ConcurrentHashMap<>());
		headers.setDestination("/foo");
		Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

		messageHandler.handleMessage(message);

		message = channel.messages.get(0);
		headers = StompHeaderAccessor.wrap(message);

		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		assertThat(headers.getDestination()).isEqualTo("/bar");
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("bar");
	}

	@Test
	public void brokerChannelCustomized() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		AbstractSubscribableChannel channel = context.getBean(
				"brokerChannel", AbstractSubscribableChannel.class);

		assertThat(channel.getInterceptors()).hasSize(4);

		ThreadPoolTaskExecutor taskExecutor = context.getBean(
				"brokerChannelExecutor", ThreadPoolTaskExecutor.class);

		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(31);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(32);
		assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(33);
	}

	@Test
	public void configureMessageConvertersDefault() {
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig();
		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();

		List<MessageConverter> converters = compositeConverter.getConverters();
		assertThat(converters).hasSize(4);
		assertThat(converters.get(0)).isInstanceOf(StringMessageConverter.class);
		assertThat(converters.get(1)).isInstanceOf(ByteArrayMessageConverter.class);
		assertThat(converters.get(2)).isInstanceOf(KotlinSerializationJsonMessageConverter.class);
		assertThat(converters.get(3)).isInstanceOf(MappingJackson2MessageConverter.class);

		ContentTypeResolver resolver = ((MappingJackson2MessageConverter) converters.get(3)).getContentTypeResolver();
		assertThat(((DefaultContentTypeResolver) resolver).getDefaultMimeType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void threadPoolSizeDefault() {
		ApplicationContext context = loadConfig(DefaultConfig.class);

		String name = "clientInboundChannelExecutor";
		ThreadPoolTaskExecutor executor = context.getBean(name, ThreadPoolTaskExecutor.class);
		assertThat(executor.getCorePoolSize()).isEqualTo(Runtime.getRuntime().availableProcessors() * 2);
		// No way to verify queue capacity

		name = "clientOutboundChannelExecutor";
		executor = context.getBean(name, ThreadPoolTaskExecutor.class);
		assertThat(executor.getCorePoolSize()).isEqualTo(Runtime.getRuntime().availableProcessors() * 2);

		name = "brokerChannelExecutor";
		executor = context.getBean(name, ThreadPoolTaskExecutor.class);
		assertThat(executor.getCorePoolSize()).isEqualTo(0);
		assertThat(executor.getMaxPoolSize()).isEqualTo(1);
	}

	@Test
	public void configureMessageConvertersCustom() {
		final MessageConverter testConverter = mock();
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
			@Override
			protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
				messageConverters.add(testConverter);
				return false;
			}
		};

		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();
		assertThat(compositeConverter.getConverters()).hasSize(1);
		Iterator<MessageConverter> iterator = compositeConverter.getConverters().iterator();
		assertThat(iterator.next()).isEqualTo(testConverter);
	}

	@Test
	public void configureMessageConvertersCustomAndDefault() {
		final MessageConverter testConverter = mock();

		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
			@Override
			protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
				messageConverters.add(testConverter);
				return true;
			}
		};
		CompositeMessageConverter compositeConverter = config.brokerMessageConverter();

		assertThat(compositeConverter.getConverters()).hasSize(5);
		Iterator<MessageConverter> iterator = compositeConverter.getConverters().iterator();
		assertThat(iterator.next()).isEqualTo(testConverter);
		assertThat(iterator.next()).isInstanceOf(StringMessageConverter.class);
		assertThat(iterator.next()).isInstanceOf(ByteArrayMessageConverter.class);
		assertThat(iterator.next()).isInstanceOf(KotlinSerializationJsonMessageConverter.class);
		assertThat(iterator.next()).isInstanceOf(MappingJackson2MessageConverter.class);
	}

	@Test
	public void customArgumentAndReturnValueTypes() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		SimpAnnotationMethodMessageHandler handler =
				context.getBean(SimpAnnotationMethodMessageHandler.class);

		List<HandlerMethodArgumentResolver> customResolvers = handler.getCustomArgumentResolvers();
		assertThat(customResolvers).hasSize(1);
		assertThat(handler.getArgumentResolvers().contains(customResolvers.get(0))).isTrue();

		List<HandlerMethodReturnValueHandler> customHandlers = handler.getCustomReturnValueHandlers();
		assertThat(customHandlers).hasSize(1);
		assertThat(handler.getReturnValueHandlers().contains(customHandlers.get(0))).isTrue();
	}

	@Test
	public void simpValidatorDefault() {
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {};
		config.setApplicationContext(new StaticApplicationContext());

		assertThat(config.simpValidator()).isNotNull();
		assertThat(config.simpValidator()).isInstanceOf(OptionalValidatorFactoryBean.class);
	}

	@Test
	public void simpValidatorCustom() {
		final Validator validator = mock();
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {
			@Override
			public Validator getValidator() {
				return validator;
			}
		};

		assertThat(config.simpValidator()).isSameAs(validator);
	}

	@Test
	public void simpValidatorMvc() {
		StaticApplicationContext appCxt = new StaticApplicationContext();
		appCxt.registerSingleton("mvcValidator", TestValidator.class);
		AbstractMessageBrokerConfiguration config = new BaseTestMessageBrokerConfig() {};
		config.setApplicationContext(appCxt);

		assertThat(config.simpValidator()).isNotNull();
		assertThat(config.simpValidator()).isInstanceOf(TestValidator.class);
	}

	@Test
	public void simpValidatorInjected() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		SimpAnnotationMethodMessageHandler messageHandler =
				context.getBean(SimpAnnotationMethodMessageHandler.class);

		assertThat(messageHandler.getValidator()).isNotNull();
	}

	@Test
	public void customPathMatcher() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		SimpleBrokerMessageHandler broker = context.getBean(SimpleBrokerMessageHandler.class);
		DefaultSubscriptionRegistry registry = (DefaultSubscriptionRegistry) broker.getSubscriptionRegistry();
		assertThat(registry.getPathMatcher().combine("a", "a")).isEqualTo("a.a");

		PathMatcher pathMatcher =
				context.getBean(SimpAnnotationMethodMessageHandler.class).getPathMatcher();

		assertThat(pathMatcher.combine("a", "a")).isEqualTo("a.a");

		DefaultUserDestinationResolver resolver = context.getBean(DefaultUserDestinationResolver.class);
		assertThat(resolver).isNotNull();
		assertThat(resolver.isRemoveLeadingSlash()).isFalse();
	}

	@Test
	public void customCacheLimit() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		SimpleBrokerMessageHandler broker = context.getBean(SimpleBrokerMessageHandler.class);
		DefaultSubscriptionRegistry registry = (DefaultSubscriptionRegistry) broker.getSubscriptionRegistry();
		assertThat(registry.getCacheLimit()).isEqualTo(8192);
	}

	@Test
	public void customUserRegistryOrder() {
		ApplicationContext context = loadConfig(CustomConfig.class);

		SimpUserRegistry registry = context.getBean(SimpUserRegistry.class);
		assertThat(registry).isInstanceOf(TestUserRegistry.class);
		assertThat(((TestUserRegistry) registry).getOrder()).isEqualTo(99);
	}

	@Test
	public void userBroadcasts() {
		ApplicationContext context = loadConfig(BrokerRelayConfig.class);

		SimpUserRegistry userRegistry = context.getBean(SimpUserRegistry.class);
		assertThat(userRegistry.getClass()).isEqualTo(MultiServerUserRegistry.class);

		UserDestinationMessageHandler handler1 = context.getBean(UserDestinationMessageHandler.class);
		assertThat(handler1.getBroadcastDestination()).isEqualTo("/topic/unresolved-user-destination");

		UserRegistryMessageHandler handler2 = context.getBean(UserRegistryMessageHandler.class);
		assertThat(handler2.getBroadcastDestination()).isEqualTo("/topic/simp-user-registry");

		StompBrokerRelayMessageHandler relay = context.getBean(StompBrokerRelayMessageHandler.class);
		assertThat(relay.getSystemSubscriptions()).isNotNull();
		assertThat(relay.getSystemSubscriptions()).hasSize(2);
		assertThat(relay.getSystemSubscriptions().get("/topic/unresolved-user-destination")).isSameAs(handler1);
		assertThat(relay.getSystemSubscriptions().get("/topic/simp-user-registry")).isSameAs(handler2);
	}

	@Test
	public void userBroadcastsDisabledWithSimpleBroker() {
		ApplicationContext context = loadConfig(SimpleBrokerConfig.class);

		SimpUserRegistry registry = context.getBean(SimpUserRegistry.class);
		assertThat(registry).isNotNull();
		assertThat(registry.getClass()).isNotEqualTo(MultiServerUserRegistry.class);

		UserDestinationMessageHandler handler = context.getBean(UserDestinationMessageHandler.class);
		assertThat((Object) handler.getBroadcastDestination()).isNull();

		Object nullBean = context.getBean("userRegistryMessageHandler");
		assertThat(nullBean.equals(null)).isTrue();
	}

	@Test // SPR-16275
	public void dotSeparatorWithBrokerSlashConvention() {
		ApplicationContext context = loadConfig(DotSeparatorWithSlashBrokerConventionConfig.class);
		testDotSeparator(context, true);
	}

	@Test // SPR-16275
	public void dotSeparatorWithBrokerDotConvention() {
		ApplicationContext context = loadConfig(DotSeparatorWithDotBrokerConventionConfig.class);
		testDotSeparator(context, false);
	}

	private void testDotSeparator(ApplicationContext context, boolean expectLeadingSlash) {
		MessageChannel inChannel = context.getBean("clientInboundChannel", MessageChannel.class);
		TestChannel outChannel = context.getBean("clientOutboundChannel", TestChannel.class);
		MessageChannel brokerChannel = context.getBean("brokerChannel", MessageChannel.class);

		inChannel.send(createConnectMessage("sess1", new long[] {0,0}));

		// 1. Subscribe to user destination

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId("sess1");
		headers.setSubscriptionId("subs1");
		headers.setDestination("/user/queue.q1");
		Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
		inChannel.send(message);

		// 2. Send message to user via inboundChannel

		headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId("sess1");
		headers.setDestination("/user/sess1/queue.q1");
		message = MessageBuilder.createMessage("123".getBytes(), headers.getMessageHeaders());
		inChannel.send(message);

		assertThat(outChannel.messages).hasSize(2);
		Message<?> outputMessage = outChannel.messages.remove(1);
		headers = StompHeaderAccessor.wrap(outputMessage);

		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		Object expecteds1 = expectLeadingSlash ? "/queue.q1-usersess1" : "queue.q1-usersess1";
		assertThat(headers.getDestination()).isEqualTo(expecteds1);
		assertThat(new String((byte[]) outputMessage.getPayload())).isEqualTo("123");
		outChannel.messages.clear();

		// 3. Send message via broker channel

		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel);
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setSessionId("sess1");
		template.convertAndSendToUser("sess1", "queue.q1", "456".getBytes(), accessor.getMessageHeaders());

		assertThat(outChannel.messages).hasSize(1);
		outputMessage = outChannel.messages.remove(0);
		headers = StompHeaderAccessor.wrap(outputMessage);

		assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
		Object expecteds = expectLeadingSlash ? "/queue.q1-usersess1" : "queue.q1-usersess1";
		assertThat(headers.getDestination()).isEqualTo(expecteds);
		assertThat(new String((byte[]) outputMessage.getPayload())).isEqualTo("456");

	}

	private AnnotationConfigApplicationContext loadConfig(Class<?> configClass) {
		return new AnnotationConfigApplicationContext(configClass);
	}

	private Message<String> createConnectMessage(String sessionId, long[] heartbeat) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
		accessor.setSessionId(sessionId);
		accessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, heartbeat);
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
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
		protected SimpUserRegistry createLocalUserRegistry(@Nullable Integer order) {
			TestUserRegistry registry = new TestUserRegistry();
			if (order != null) {
				registry.setOrder(order);
			}
			return registry;
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
		public AbstractSubscribableChannel clientInboundChannel(TaskExecutor clientInboundChannelExecutor) {
			return new TestChannel();
		}

		@Override
		@Bean
		public AbstractSubscribableChannel clientOutboundChannel(TaskExecutor clientOutboundChannelExecutor) {
			return new TestChannel();
		}

		@Override
		@Bean
		public AbstractSubscribableChannel brokerChannel(AbstractSubscribableChannel clientInboundChannel,
				AbstractSubscribableChannel clientOutboundChannel, TaskExecutor brokerChannelExecutor) {
			return new TestChannel();
		}
	}


	@Configuration
	static class BrokerRelayConfig extends SimpleBrokerConfig {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableStompBrokerRelay("/topic", "/queue")
					.setAutoStartup(true)
					.setTcpClient(new NoOpTcpClient())
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
			registration.interceptors(this.interceptor);
			registration.taskExecutor(new CustomThreadPoolTaskExecutor())
					.corePoolSize(11).maxPoolSize(12).keepAliveSeconds(13).queueCapacity(14);
		}

		@Override
		protected void configureClientOutboundChannel(ChannelRegistration registration) {
			registration.interceptors(this.interceptor, this.interceptor);
			registration.taskExecutor().corePoolSize(21).maxPoolSize(22).keepAliveSeconds(23).queueCapacity(24);
		}

		@Override
		protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(mock());
		}

		@Override
		protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(mock());
		}

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.configureBrokerChannel().interceptors(this.interceptor, this.interceptor, this.interceptor);
			registry.configureBrokerChannel().taskExecutor()
					.corePoolSize(31).maxPoolSize(32).keepAliveSeconds(33).queueCapacity(34);
			registry.setPathMatcher(new AntPathMatcher(".")).enableSimpleBroker("/topic", "/queue");
			registry.setCacheLimit(8192);
			registry.setPreservePublishOrder(true);
			registry.setUserRegistryOrder(99);
		}
	}


	@Configuration
	static abstract class BaseDotSeparatorConfig extends BaseTestMessageBrokerConfig {

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setPathMatcher(new AntPathMatcher("."));
		}

		@Override
		@Bean
		public AbstractSubscribableChannel clientInboundChannel(TaskExecutor clientInboundChannelExecutor) {
			// synchronous
			return new ExecutorSubscribableChannel(null);
		}

		@Override
		@Bean
		public AbstractSubscribableChannel clientOutboundChannel(TaskExecutor clientOutboundChannelExecutor) {
			return new TestChannel();
		}

		@Override
		@Bean
		public AbstractSubscribableChannel brokerChannel(AbstractSubscribableChannel clientInboundChannel,
				AbstractSubscribableChannel clientOutboundChannel, TaskExecutor brokerChannelExecutor) {
			// synchronous
			return new ExecutorSubscribableChannel(null);
		}
	}

	@Configuration
	static class DotSeparatorWithSlashBrokerConventionConfig extends BaseDotSeparatorConfig {

		// RabbitMQ-style broker convention for STOMP destinations

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			super.configureMessageBroker(registry);
			registry.enableSimpleBroker("/topic", "/queue");
		}
	}

	@Configuration
	static class DotSeparatorWithDotBrokerConventionConfig extends BaseDotSeparatorConfig {

		// Artemis-style broker convention for STOMP destinations

		@Override
		protected void configureMessageBroker(MessageBrokerRegistry registry) {
			super.configureMessageBroker(registry);
			registry.enableSimpleBroker("topic.", "queue.");
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


	private static class TestUserRegistry implements SimpUserRegistry, Ordered {

		private Integer order;


		public void setOrder(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public SimpUser getUser(String userName) { return null; }

		@Override
		public Set<SimpUser> getUsers() { return null; }

		@Override
		public int getUserCount() { return 0; }

		@Override
		public Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher) { return null; }
	}


	private static class TestValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}


	@SuppressWarnings("serial")
	private static class CustomThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
	}


	private static class NoOpTcpClient implements TcpOperations<byte[]> {

		@Override
		public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler, ReconnectStrategy strategy) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> shutdownAsync() {
			return CompletableFuture.completedFuture(null);
		}

	}

}
