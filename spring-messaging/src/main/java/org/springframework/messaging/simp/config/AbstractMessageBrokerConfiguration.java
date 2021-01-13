/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.converter.JsonbMessageConverter;
import org.springframework.messaging.converter.KotlinSerializationJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Provides essential configuration for handling messages with simple messaging
 * protocols such as STOMP.
 *
 * <p>{@link #clientInboundChannel(TaskExecutor)} and {@link #clientOutboundChannel(TaskExecutor)} deliver
 * messages to and from remote clients to several message handlers such as the
 * following.
 * <ul>
 * <li>{@link #simpAnnotationMethodMessageHandler(AbstractSubscribableChannel, AbstractSubscribableChannel, SimpMessagingTemplate, CompositeMessageConverter)}</li>
 * <li>{@link #simpleBrokerMessageHandler(AbstractSubscribableChannel, AbstractSubscribableChannel, AbstractSubscribableChannel, UserDestinationResolver)}</li>
 * <li>{@link #stompBrokerRelayMessageHandler(AbstractSubscribableChannel, AbstractSubscribableChannel, AbstractSubscribableChannel, UserDestinationMessageHandler, MessageHandler, UserDestinationResolver)}</li>
 * <li>{@link #userDestinationMessageHandler(AbstractSubscribableChannel, AbstractSubscribableChannel, AbstractSubscribableChannel, UserDestinationResolver)}</li>
 * </ul>
 *
 * <p>{@link #brokerChannel(AbstractSubscribableChannel, AbstractSubscribableChannel, TaskExecutor)} delivers messages from within the application to the
 * the respective message handlers. {@link #brokerMessagingTemplate(AbstractSubscribableChannel, AbstractSubscribableChannel, AbstractSubscribableChannel, CompositeMessageConverter)} can be injected
 * into any application component to send messages.
 *
 * <p>Subclasses are responsible for the parts of the configuration that feed messages
 * to and from the client inbound/outbound channels (e.g. STOMP over WebSocket).
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractMessageBrokerConfiguration implements ApplicationContextAware {

	private static final String MVC_VALIDATOR_NAME = "mvcValidator";

	private static final boolean jackson2Present;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationJsonPresent;


	static {
		ClassLoader classLoader = AbstractMessageBrokerConfiguration.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ChannelRegistration clientInboundChannelRegistration;

	@Nullable
	private ChannelRegistration clientOutboundChannelRegistration;

	@Nullable
	private MessageBrokerRegistry brokerRegistry;


	/**
	 * Protected constructor.
	 */
	protected AbstractMessageBrokerConfiguration() {
	}


	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public AbstractSubscribableChannel clientInboundChannel(TaskExecutor clientInboundChannelExecutor) {
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(clientInboundChannelExecutor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration reg = getClientInboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public TaskExecutor clientInboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientInboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientInboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientInboundChannelRegistration() {
		if (this.clientInboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientInboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientInboundChannelRegistration = registration;
		}
		return this.clientInboundChannelRegistration;
	}

	/**
	 * A hook for subclasses to customize the message channel for inbound messages
	 * from WebSocket clients.
	 */
	protected void configureClientInboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel(TaskExecutor clientOutboundChannelExecutor) {
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(clientOutboundChannelExecutor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration reg = getClientOutboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public TaskExecutor clientOutboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientOutboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientOutboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientOutboundChannelRegistration() {
		if (this.clientOutboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientOutboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientOutboundChannelRegistration = registration;
		}
		return this.clientOutboundChannelRegistration;
	}

	/**
	 * A hook for subclasses to customize the message channel for messages from
	 * the application or message broker to WebSocket clients.
	 */
	protected void configureClientOutboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel brokerChannel(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, TaskExecutor brokerChannelExecutor) {
		ChannelRegistration reg = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getBrokerChannelRegistration();
		ExecutorSubscribableChannel channel = (reg.hasTaskExecutor() ?
				new ExecutorSubscribableChannel(brokerChannelExecutor) : new ExecutorSubscribableChannel());
		reg.interceptors(new ImmutableMessageChannelInterceptor());
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		channel.setInterceptors(reg.getInterceptors());
		return channel;
	}

	@Bean
	public TaskExecutor brokerChannelExecutor(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {
		ChannelRegistration reg = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getBrokerChannelRegistration();
		ThreadPoolTaskExecutor executor;
		if (reg.hasTaskExecutor()) {
			executor = reg.taskExecutor().getTaskExecutor();
		}
		else {
			// Should never be used
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(0);
			executor.setMaxPoolSize(1);
			executor.setQueueCapacity(0);
		}
		executor.setThreadNamePrefix("brokerChannel-");
		return executor;
	}

	/**
	 * An accessor for the {@link MessageBrokerRegistry} that ensures its one-time creation
	 * and initialization through {@link #configureMessageBroker(MessageBrokerRegistry)}.
	 */
	protected final MessageBrokerRegistry getBrokerRegistry(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {
		if (this.brokerRegistry == null) {
			MessageBrokerRegistry registry = new MessageBrokerRegistry(clientInboundChannel, clientOutboundChannel);
			configureMessageBroker(registry);
			this.brokerRegistry = registry;
		}
		return this.brokerRegistry;
	}

	/**
	 * A hook for subclasses to customize message broker configuration through the
	 * provided {@link MessageBrokerRegistry} instance.
	 */
	protected void configureMessageBroker(MessageBrokerRegistry registry) {
	}

	/**
	 * Provide access to the configured PatchMatcher for access from other
	 * configuration classes.
	 */
	@Nullable
	public final PathMatcher getPathMatcher(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {
		return getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getPathMatcher();
	}

	@Bean
	public SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate, CompositeMessageConverter brokerMessageConverter) {
		SimpAnnotationMethodMessageHandler handler = createAnnotationMethodMessageHandler(clientInboundChannel,
				clientOutboundChannel, brokerMessagingTemplate);
		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		handler.setDestinationPrefixes(brokerRegistry.getApplicationDestinationPrefixes());
		handler.setMessageConverter(brokerMessageConverter);
		handler.setValidator(simpValidator());

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
		addArgumentResolvers(argumentResolvers);
		handler.setCustomArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
		addReturnValueHandlers(returnValueHandlers);
		handler.setCustomReturnValueHandlers(returnValueHandlers);

		PathMatcher pathMatcher = brokerRegistry.getPathMatcher();
		if (pathMatcher != null) {
			handler.setPathMatcher(pathMatcher);
		}
		return handler;
	}

	/**
	 * Protected method for plugging in a custom subclass of
	 * {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
	 * SimpAnnotationMethodMessageHandler}.
	 * @since 5.3.2
	 */
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate) {
		return new SimpAnnotationMethodMessageHandler(clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, AbstractSubscribableChannel brokerChannel,
			UserDestinationResolver userDestinationResolver) {
		SimpleBrokerMessageHandler handler = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getSimpleBroker(brokerChannel);
		if (handler == null) {
			return null;
		}
		updateUserDestinationResolver(handler, userDestinationResolver);
		return handler;
	}

	private void updateUserDestinationResolver(AbstractBrokerMessageHandler handler, UserDestinationResolver userDestinationResolver) {
		Collection<String> prefixes = handler.getDestinationPrefixes();
		if (!prefixes.isEmpty() && !prefixes.iterator().next().startsWith("/")) {
			((DefaultUserDestinationResolver) userDestinationResolver).setRemoveLeadingSlash(true);
		}
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, AbstractSubscribableChannel brokerChannel,
			UserDestinationMessageHandler userDestinationMessageHandler, @Nullable MessageHandler userRegistryMessageHandler,
			UserDestinationResolver userDestinationResolver) {
		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		StompBrokerRelayMessageHandler handler = brokerRegistry.getStompBrokerRelay(brokerChannel);
		if (handler == null) {
			return null;
		}
		Map<String, MessageHandler> subscriptions = new HashMap<>(4);
		String destination = brokerRegistry.getUserDestinationBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userDestinationMessageHandler);
		}
		destination = brokerRegistry.getUserRegistryBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userRegistryMessageHandler);
		}
		handler.setSystemSubscriptions(subscriptions);
		updateUserDestinationResolver(handler, userDestinationResolver);
		return handler;
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, AbstractSubscribableChannel brokerChannel,
			UserDestinationResolver userDestinationResolver) {
		UserDestinationMessageHandler handler = new UserDestinationMessageHandler(clientInboundChannel,
				brokerChannel, userDestinationResolver);
		String destination = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getUserDestinationBroadcast();
		if (destination != null) {
			handler.setBroadcastDestination(destination);
		}
		return handler;
	}

	@Bean
	@Nullable
	public MessageHandler userRegistryMessageHandler(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, SimpUserRegistry userRegistry,
			SimpMessagingTemplate brokerMessagingTemplate, TaskScheduler messageBrokerTaskScheduler) {
		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		if (brokerRegistry.getUserRegistryBroadcast() == null) {
			return null;
		}
		Assert.isInstanceOf(MultiServerUserRegistry.class, userRegistry, "MultiServerUserRegistry required");
		return new UserRegistryMessageHandler((MultiServerUserRegistry) userRegistry,
				brokerMessagingTemplate, brokerRegistry.getUserRegistryBroadcast(),
				messageBrokerTaskScheduler);
	}

	// Expose alias for 4.1 compatibility
	@Bean(name = {"messageBrokerTaskScheduler", "messageBrokerSockJsTaskScheduler"})
	public TaskScheduler messageBrokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MessageBroker-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Bean
	public SimpMessagingTemplate brokerMessagingTemplate(AbstractSubscribableChannel brokerChannel,
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			CompositeMessageConverter brokerMessageConverter) {
		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel);
		String prefix = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getUserDestinationPrefix();
		if (prefix != null) {
			template.setUserDestinationPrefix(prefix);
		}
		template.setMessageConverter(brokerMessageConverter);
		return template;
	}

	@Bean
	public CompositeMessageConverter brokerMessageConverter() {
		List<MessageConverter> converters = new ArrayList<>();
		boolean registerDefaults = configureMessageConverters(converters);
		if (registerDefaults) {
			converters.add(new StringMessageConverter());
			converters.add(new ByteArrayMessageConverter());
			if (jackson2Present) {
				converters.add(createJacksonConverter());
			}
			else if (gsonPresent) {
				converters.add(new GsonMessageConverter());
			}
			else if (jsonbPresent) {
				converters.add(new JsonbMessageConverter());
			}
			else if (kotlinSerializationJsonPresent) {
				converters.add(new KotlinSerializationJsonMessageConverter());
			}
		}
		return new CompositeMessageConverter(converters);
	}

	protected MappingJackson2MessageConverter createJacksonConverter() {
		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
		resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setContentTypeResolver(resolver);
		return converter;
	}

	/**
	 * Override this method to add custom message converters.
	 * @param messageConverters the list to add converters to, initially empty
	 * @return {@code true} if default message converters should be added to list,
	 * {@code false} if no more converters should be added
	 */
	protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return true;
	}

	@Bean
	public UserDestinationResolver userDestinationResolver(SimpUserRegistry userRegistry,
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {
		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(userRegistry);
		String prefix = getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getUserDestinationPrefix();
		if (prefix != null) {
			resolver.setUserDestinationPrefix(prefix);
		}
		return resolver;
	}

	@Bean
	@SuppressWarnings("deprecation")
	public SimpUserRegistry userRegistry(AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {
		SimpUserRegistry registry = createLocalUserRegistry();
		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		if (registry == null) {
			registry = createLocalUserRegistry(brokerRegistry.getUserRegistryOrder());
		}
		boolean broadcast = brokerRegistry.getUserRegistryBroadcast() != null;
		return (broadcast ? new MultiServerUserRegistry(registry) : registry);
	}

	/**
	 * Create the user registry that provides access to local users.
	 * @deprecated as of 5.1 in favor of {@link #createLocalUserRegistry(Integer)}
	 */
	@Deprecated
	@Nullable
	protected SimpUserRegistry createLocalUserRegistry() {
		return null;
	}

	/**
	 * Create the user registry that provides access to local users.
	 * @param order the order to use as a {@link SmartApplicationListener}.
	 * @since 5.1
	 */
	protected abstract SimpUserRegistry createLocalUserRegistry(@Nullable Integer order);

	/**
	 * Return an {@link org.springframework.validation.Validator} instance for
	 * validating {@code @Payload} method arguments.
	 * <p>In order, this method tries to get a Validator instance:
	 * <ul>
	 * <li>delegating to getValidator() first</li>
	 * <li>if none returned, getting an existing instance with its well-known name "mvcValidator",
	 * created by an MVC configuration</li>
	 * <li>if none returned, checking the classpath for the presence of a JSR-303 implementation
	 * before creating a {@code OptionalValidatorFactoryBean}</li>
	 * <li>returning a no-op Validator instance</li>
	 * </ul>
	 */
	protected Validator simpValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (this.applicationContext != null && this.applicationContext.containsBean(MVC_VALIDATOR_NAME)) {
				validator = this.applicationContext.getBean(MVC_VALIDATOR_NAME, Validator.class);
			}
			else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, AbstractMessageBrokerConfiguration.class.getClassLoader());
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Could not find default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new Validator() {
					@Override
					public boolean supports(Class<?> clazz) {
						return false;
					}
					@Override
					public void validate(@Nullable Object target, Errors errors) {
					}
				};
			}
		}
		return validator;
	}

	/**
	 * Override this method to provide a custom {@link Validator}.
	 * @since 4.0.1
	 */
	@Nullable
	public Validator getValidator() {
		return null;
	}

}
