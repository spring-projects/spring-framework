/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.SmartApplicationListener;
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
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

/**
 * Provides essential configuration for handling messages with simple messaging
 * protocols such as STOMP.
 *
 * <p>{@link #clientInboundChannel} and {@link #clientOutboundChannel} deliver
 * messages to and from remote clients to several message handlers such as the
 * following.
 * <ul>
 * <li>{@link #simpAnnotationMethodMessageHandler}</li>
 * <li>{@link #simpleBrokerMessageHandler}</li>
 * <li>{@link #stompBrokerRelayMessageHandler}</li>
 * <li>{@link #userDestinationMessageHandler}</li>
 * </ul>
 *
 * <p>{@link #brokerChannel} delivers messages from within the application to the
 * respective message handlers. {@link #brokerMessagingTemplate} can be injected
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
		jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
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

	@Nullable
	private Integer phase;


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
	public AbstractSubscribableChannel clientInboundChannel(
			@Qualifier("clientInboundChannelExecutor") Executor executor) {

		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(executor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration reg = getClientInboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public Executor clientInboundChannelExecutor() {
		ChannelRegistration registration = getClientInboundChannelRegistration();
		Executor executor = getExecutor(registration, "clientInboundChannel-", this::defaultExecutor);
		if (executor instanceof ExecutorConfigurationSupport executorSupport) {
			executorSupport.setPhase(getPhase());
		}
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

	protected final int getPhase() {
		if (this.phase == null) {
			this.phase = initPhase();
		}
		return this.phase;
	}

	protected int initPhase() {
		return 0;
	}

	/**
	 * A hook for subclasses to customize the message channel for inbound messages
	 * from WebSocket clients.
	 */
	protected void configureClientInboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel(
			@Qualifier("clientOutboundChannelExecutor") Executor executor) {

		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(executor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration registration = getClientOutboundChannelRegistration();
		if (registration.hasInterceptors()) {
			channel.setInterceptors(registration.getInterceptors());
		}
		return channel;
	}

	@Bean
	public Executor clientOutboundChannelExecutor() {
		ChannelRegistration registration = getClientOutboundChannelRegistration();
		Executor executor = getExecutor(registration, "clientOutboundChannel-", this::defaultExecutor);
		if (executor instanceof ExecutorConfigurationSupport executorSupport) {
			executorSupport.setPhase(getPhase());
		}
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
	public AbstractSubscribableChannel brokerChannel(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			@Qualifier("brokerChannelExecutor") Executor executor) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		ChannelRegistration registration = registry.getBrokerChannelRegistration();
		ExecutorSubscribableChannel channel = (registration.hasExecutor() ?
				new ExecutorSubscribableChannel(executor) : new ExecutorSubscribableChannel());
		registration.interceptors(new ImmutableMessageChannelInterceptor());
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		channel.setInterceptors(registration.getInterceptors());
		return channel;
	}

	@Bean
	public Executor brokerChannelExecutor(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		ChannelRegistration registration = registry.getBrokerChannelRegistration();
		Executor executor = getExecutor(registration, "brokerChannel-", () -> {
			// Should never be used
			ThreadPoolTaskExecutor fallbackExecutor = new ThreadPoolTaskExecutor();
			fallbackExecutor.setCorePoolSize(0);
			fallbackExecutor.setMaxPoolSize(1);
			fallbackExecutor.setQueueCapacity(0);
			return fallbackExecutor;
		});
		if (executor instanceof ExecutorConfigurationSupport executorSupport) {
			executorSupport.setPhase(getPhase());
		}
		return executor;
	}

	private Executor defaultExecutor() {
		return new TaskExecutorRegistration().getTaskExecutor();
	}

	private static Executor getExecutor(ChannelRegistration registration,
			String threadNamePrefix, Supplier<Executor> fallback) {

		return registration.getExecutor(fallback,
				executor -> setThreadNamePrefix(executor, threadNamePrefix));
	}

	private static void setThreadNamePrefix(Executor executor, String name) {
		if (executor instanceof CustomizableThreadCreator ctc) {
			ctc.setThreadNamePrefix(name);
		}
	}

	/**
	 * An accessor for the {@link MessageBrokerRegistry} that ensures its one-time creation
	 * and initialization through {@link #configureMessageBroker(MessageBrokerRegistry)}.
	 */
	protected final MessageBrokerRegistry getBrokerRegistry(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

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
	public final PathMatcher getPathMatcher(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		return getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getPathMatcher();
	}

	@Bean
	public SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate, CompositeMessageConverter brokerMessageConverter) {

		SimpAnnotationMethodMessageHandler handler = createAnnotationMethodMessageHandler(
						clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);

		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		handler.setDestinationPrefixes(brokerRegistry.getApplicationDestinationPrefixes());
		handler.setMessageConverter(brokerMessageConverter);
		handler.setValidator(simpValidator());
		handler.setPhase(getPhase());

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

		SimpAnnotationMethodMessageHandler handler = new SimpAnnotationMethodMessageHandler(
				clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);

		handler.setPhase(getPhase());
		return handler;
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		SimpleBrokerMessageHandler handler = registry.getSimpleBroker(brokerChannel);
		if (handler == null) {
			return null;
		}
		updateUserDestinationResolver(handler, userDestinationResolver, registry.getUserDestinationPrefix());
		handler.setPhase(getPhase());
		return handler;
	}

	private void updateUserDestinationResolver(
			AbstractBrokerMessageHandler handler, UserDestinationResolver userDestinationResolver,
			@Nullable String userDestinationPrefix) {

		Collection<String> prefixes = handler.getDestinationPrefixes();
		if (!prefixes.isEmpty() && !prefixes.iterator().next().startsWith("/")) {
			((DefaultUserDestinationResolver) userDestinationResolver).setRemoveLeadingSlash(true);
		}
		if (StringUtils.hasText(userDestinationPrefix)) {
			handler.setUserDestinationPredicate(destination -> destination.startsWith(userDestinationPrefix));
		}
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationMessageHandler userDestinationMessageHandler,
			@Nullable MessageHandler userRegistryMessageHandler, UserDestinationResolver userDestinationResolver) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		StompBrokerRelayMessageHandler handler = registry.getStompBrokerRelay(brokerChannel);
		if (handler == null) {
			return null;
		}
		Map<String, MessageHandler> subscriptions = new HashMap<>(4);
		String destination = registry.getUserDestinationBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userDestinationMessageHandler);
		}
		destination = registry.getUserRegistryBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userRegistryMessageHandler);
		}
		handler.setSystemSubscriptions(subscriptions);
		updateUserDestinationResolver(handler, userDestinationResolver, registry.getUserDestinationPrefix());
		handler.setPhase(getPhase());
		return handler;
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		UserDestinationMessageHandler handler =
				new UserDestinationMessageHandler(clientInboundChannel, brokerChannel, userDestinationResolver);

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String destination = registry.getUserDestinationBroadcast();
		if (destination != null) {
			handler.setBroadcastDestination(destination);
		}
		handler.setPhase(getPhase());
		return handler;
	}

	@Bean
	@Nullable
	public MessageHandler userRegistryMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpUserRegistry userRegistry, SimpMessagingTemplate brokerMessagingTemplate,
			@Qualifier("messageBrokerTaskScheduler") TaskScheduler scheduler) {

		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		if (brokerRegistry.getUserRegistryBroadcast() == null) {
			return null;
		}
		Assert.isInstanceOf(MultiServerUserRegistry.class, userRegistry, "MultiServerUserRegistry required");
		return new UserRegistryMessageHandler((MultiServerUserRegistry) userRegistry,
				brokerMessagingTemplate, brokerRegistry.getUserRegistryBroadcast(),
				scheduler);
	}

	// Expose alias for 4.1 compatibility
	@Bean(name = {"messageBrokerTaskScheduler", "messageBrokerSockJsTaskScheduler"})
	public TaskScheduler messageBrokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MessageBroker-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		scheduler.setPhase(getPhase());
		return scheduler;
	}

	@Bean
	public SimpMessagingTemplate brokerMessagingTemplate(
			AbstractSubscribableChannel brokerChannel, AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, CompositeMessageConverter brokerMessageConverter) {

		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel);
		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String prefix = registry.getUserDestinationPrefix();
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
			if (kotlinSerializationJsonPresent) {
				converters.add(new KotlinSerializationJsonMessageConverter());
			}
			if (jackson2Present) {
				converters.add(createJacksonConverter());
			}
			else if (gsonPresent) {
				converters.add(new GsonMessageConverter());
			}
			else if (jsonbPresent) {
				converters.add(new JsonbMessageConverter());
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
	public UserDestinationResolver userDestinationResolver(
			SimpUserRegistry userRegistry, AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {

		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(userRegistry);
		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String prefix = registry.getUserDestinationPrefix();
		if (prefix != null) {
			resolver.setUserDestinationPrefix(prefix);
		}
		return resolver;
	}

	@Bean
	public SimpUserRegistry userRegistry(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		SimpUserRegistry userRegistry = createLocalUserRegistry(brokerRegistry.getUserRegistryOrder());
		boolean broadcast = brokerRegistry.getUserRegistryBroadcast() != null;
		return (broadcast ? new MultiServerUserRegistry(userRegistry) : userRegistry);
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
			else if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
				try {
					validator = new OptionalValidatorFactoryBean();
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Failed to create default validator", ex);
				}
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
