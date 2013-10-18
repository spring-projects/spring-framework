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

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.handler.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.handler.AnnotationMethodMessageHandler;
import org.springframework.messaging.simp.handler.MutableUserQueueSuffixResolver;
import org.springframework.messaging.simp.handler.SimpleUserQueueSuffixResolver;
import org.springframework.messaging.simp.handler.UserDestinationMessageHandler;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.messaging.support.converter.ByteArrayMessageConverter;
import org.springframework.messaging.support.converter.CompositeMessageConverter;
import org.springframework.messaging.support.converter.DefaultContentTypeResolver;
import org.springframework.messaging.support.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.messaging.support.converter.StringMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.config.SockJsServiceRegistration;


/**
 * Configuration support for broker-backed messaging over WebSocket using a higher-level
 * messaging sub-protocol such as STOMP. This class can either be extended directly
 * or its configuration can also be customized in a callback style via
 * {@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker} and
 * {@link WebSocketMessageBrokerConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class WebSocketMessageBrokerConfigurationSupport {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", WebMvcConfigurationSupport.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", WebMvcConfigurationSupport.class.getClassLoader());

	private MessageBrokerConfigurer messageBrokerConfigurer;


	// WebSocket configuration including message channels to/from the application

	@Bean
	public HandlerMapping brokerWebSocketHandlerMapping() {
		ServletStompEndpointRegistry registry = new ServletStompEndpointRegistry(subProtocolWebSocketHandler(),
				userQueueSuffixResolver(), brokerDefaultSockJsTaskScheduler());

		registerStompEndpoints(registry);
		AbstractHandlerMapping hm = registry.getHandlerMapping();
		hm.setOrder(1);
		return hm;
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler() {
		SubProtocolWebSocketHandler wsHandler = new SubProtocolWebSocketHandler(webSocketRequestChannel());
		webSocketResponseChannel().subscribe(wsHandler);
		return wsHandler;
	}

	@Bean
	public MutableUserQueueSuffixResolver userQueueSuffixResolver() {
		return new SimpleUserQueueSuffixResolver();
	}

	/**
	 * The default TaskScheduler to use if none is configured via
	 * {@link SockJsServiceRegistration#setTaskScheduler()}, i.e.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableWebSocketMessageBroker
	 * public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	 *
	 *   public void registerStompEndpoints(StompEndpointRegistry registry) {
	 *     registry.addEndpoint("/stomp").withSockJS().setTaskScheduler(myScheduler());
	 *   }
	 *
	 *   // ...
	 *
	 * }
	 * </pre>
	 */
	@Bean
	public ThreadPoolTaskScheduler brokerDefaultSockJsTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("BrokerSockJS-");
		return scheduler;
	}

	protected void registerStompEndpoints(StompEndpointRegistry registry) {
	}

	@Bean
	public SubscribableChannel webSocketRequestChannel() {
		return new ExecutorSubscribableChannel(webSocketChannelExecutor());
	}

	@Bean
	public SubscribableChannel webSocketResponseChannel() {
		return new ExecutorSubscribableChannel(webSocketChannelExecutor());
	}

	@Bean
	public ThreadPoolTaskExecutor webSocketChannelExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("BrokerWebSocketChannel-");
		return executor;
	}

	// Handling of messages by the application

	@Bean
	public AnnotationMethodMessageHandler annotationMethodMessageHandler() {
		AnnotationMethodMessageHandler handler =
				new AnnotationMethodMessageHandler(brokerMessagingTemplate(), webSocketResponseChannel());
		handler.setDestinationPrefixes(getMessageBrokerConfigurer().getAnnotationMethodDestinationPrefixes());
		handler.setMessageConverter(simpMessageConverter());
		webSocketRequestChannel().subscribe(handler);
		return handler;
	}

	@Bean
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler() {
		AbstractBrokerMessageHandler handler = getMessageBrokerConfigurer().getSimpleBroker();
		if (handler == null) {
			return noopBroker;
		}
		else {
			webSocketRequestChannel().subscribe(handler);
			brokerChannel().subscribe(handler);
			return handler;
		}
	}

	@Bean
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler() {
		AbstractBrokerMessageHandler handler = getMessageBrokerConfigurer().getStompBrokerRelay();
		if (handler == null) {
			return noopBroker;
		}
		else {
			webSocketRequestChannel().subscribe(handler);
			brokerChannel().subscribe(handler);
			return handler;
		}
	}

	protected final MessageBrokerConfigurer getMessageBrokerConfigurer() {
		if (this.messageBrokerConfigurer == null) {
			MessageBrokerConfigurer configurer = new MessageBrokerConfigurer(webSocketResponseChannel());
			configureMessageBroker(configurer);
			this.messageBrokerConfigurer = configurer;
		}
		return this.messageBrokerConfigurer;
	}

	protected void configureMessageBroker(MessageBrokerConfigurer configurer) {
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler() {
		UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
				brokerMessagingTemplate(), userQueueSuffixResolver());
		webSocketRequestChannel().subscribe(handler);
		brokerChannel().subscribe(handler);
		return handler;
	}

	@Bean
	public SimpMessageSendingOperations brokerMessagingTemplate() {
		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel());
		template.setMessageConverter(simpMessageConverter());
		return template;
	}

	@Bean
	public SubscribableChannel brokerChannel() {
		return new ExecutorSubscribableChannel(); // synchronous
	}

	@Bean
	public CompositeMessageConverter simpMessageConverter() {

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
