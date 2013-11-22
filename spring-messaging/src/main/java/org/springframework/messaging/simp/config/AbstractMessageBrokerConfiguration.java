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

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.handler.*;
import org.springframework.messaging.support.channel.AbstractSubscribableChannel;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.messaging.support.converter.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Provides essential configuration for handling messages with simple messaging
 * protocols such as STOMP.
 * <p>
 * {@link #clientInboundChannel()} and {@link #clientOutboundChannel()} deliver messages
 * to and from remote clients to several message handlers such as
 * <ul>
 *	<li>{@link #simpAnnotationMethodMessageHandler()}</li>
 * 	<li>{@link #simpleBrokerMessageHandler()}</li>
 *	<li>{@link #stompBrokerRelayMessageHandler()}</li>
 *	<li>{@link #userDestinationMessageHandler()}</li>
 * </ul>
 * while {@link #brokerChannel()} delivers messages from within the application to the
 * the respective message handlers. {@link #brokerMessagingTemplate()} can be injected
 * into any application component to send messages.
 * <p>
 * Sub-classes are responsible for the part of the configuration that feed messages
 * to and from the client inbound/outbound channels (e.g. STOMP over WebSokcet).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageBrokerConfiguration {

	private static final boolean jackson2Present= ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", AbstractMessageBrokerConfiguration.class.getClassLoader());


	private MessageBrokerRegistry brokerRegistry;


	/**
	 * Protected constructor.
	 */
	protected AbstractMessageBrokerConfiguration() {
	}


	/**
	 * An accessor for the {@link MessageBrokerRegistry} that ensures its one-time creation
	 * and initialization through {@link #configureMessageBroker(MessageBrokerRegistry)}.
	 */
	protected final MessageBrokerRegistry getBrokerRegistry() {
		if (this.brokerRegistry == null) {
			MessageBrokerRegistry registry = new MessageBrokerRegistry(clientOutboundChannel());
			configureMessageBroker(registry);
			this.brokerRegistry = registry;
		}
		return this.brokerRegistry;
	}

	/**
	 * A hook for sub-classes to customize message broker configuration through the
	 * provided {@link MessageBrokerRegistry} instance.
	 */
	protected abstract void configureMessageBroker(MessageBrokerRegistry registry);


	@Bean
	public AbstractSubscribableChannel clientInboundChannel() {
		return new ExecutorSubscribableChannel(clientInboundChannelExecutor());
	}

	@Bean
	public ThreadPoolTaskExecutor clientInboundChannelExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("ClientInboundChannel-");
		return executor;
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel() {
		return new ExecutorSubscribableChannel(clientOutboundChannelExecutor());
	}

	@Bean
	public ThreadPoolTaskExecutor clientOutboundChannelExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("ClientOutboundChannel-");
		return executor;
	}

	@Bean
	public AbstractSubscribableChannel brokerChannel() {
		return new ExecutorSubscribableChannel(); // synchronous
	}


	@Bean
	public SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler() {

		SimpAnnotationMethodMessageHandler handler =
				new SimpAnnotationMethodMessageHandler(brokerMessagingTemplate(), clientOutboundChannel());

		handler.setDestinationPrefixes(getBrokerRegistry().getApplicationDestinationPrefixes());
		handler.setMessageConverter(brokerMessageConverter());
		clientInboundChannel().subscribe(handler);
		return handler;
	}

	@Bean
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler() {
		SimpleBrokerMessageHandler handler = getBrokerRegistry().getSimpleBroker();
		if (handler != null) {
			clientInboundChannel().subscribe(handler);
			brokerChannel().subscribe(handler);
			return handler;
		}
		return noopBroker;
	}

	@Bean
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler() {
		AbstractBrokerMessageHandler handler = getBrokerRegistry().getStompBrokerRelay();
		if (handler != null) {
			clientInboundChannel().subscribe(handler);
			brokerChannel().subscribe(handler);
			return handler;
		}
		return noopBroker;
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler() {

		UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
				brokerMessagingTemplate(), userDestinationResolver());

		clientInboundChannel().subscribe(handler);
		brokerChannel().subscribe(handler);
		return handler;
	}

	@Bean
	public SimpMessagingTemplate brokerMessagingTemplate() {
		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel());
		String prefix = getBrokerRegistry().getUserDestinationPrefix();
		if (prefix != null) {
			template.setUserDestinationPrefix(prefix);
		}
		template.setMessageConverter(brokerMessageConverter());
		return template;
	}

	@Bean
	public CompositeMessageConverter brokerMessageConverter() {

		DefaultContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();

		List<MessageConverter> converters = new ArrayList<MessageConverter>();
		if (jackson2Present) {
			converters.add(new MappingJackson2MessageConverter());
			contentTypeResolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		}
		converters.add(new StringMessageConverter());
		converters.add(new ByteArrayMessageConverter());

		return new CompositeMessageConverter(converters, contentTypeResolver);
	}

	@Bean
	public UserDestinationResolver userDestinationResolver() {
		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(userSessionRegistry());
		String prefix = getBrokerRegistry().getUserDestinationPrefix();
		if (prefix != null) {
			resolver.setUserDestinationPrefix(prefix);
		}
		return resolver;
	}

	@Bean
	public UserSessionRegistry userSessionRegistry() {
		return new DefaultUserSessionRegistry();
	}


	private static final AbstractBrokerMessageHandler noopBroker = new AbstractBrokerMessageHandler(null) {

		@Override
		protected void startInternal() {
		}
		@Override
		protected void stopInternal() {
		}
		@Override
		protected void handleMessageInternal(Message<?> message) {
		}
	};

}
