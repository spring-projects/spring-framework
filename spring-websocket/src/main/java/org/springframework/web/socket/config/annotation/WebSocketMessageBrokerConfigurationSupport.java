/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.socket.config.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpSessionScope;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.OrderedMessageChannelDecorator;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * Extends {@link AbstractMessageBrokerConfiguration} and adds configuration for
 * receiving and responding to STOMP messages from WebSocket clients.
 *
 * <p>Typically used in conjunction with
 * {@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker} but can
 * also be extended directly.
 *
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class WebSocketMessageBrokerConfigurationSupport extends AbstractMessageBrokerConfiguration {

	private @Nullable WebSocketTransportRegistration transportRegistration;


	@Override
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel,AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate) {

		WebSocketAnnotationMethodMessageHandler handler = new WebSocketAnnotationMethodMessageHandler(
				clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);

		handler.setPhase(getPhase());
		return handler;
	}

	@Override
	protected SimpUserRegistry createLocalUserRegistry(@Nullable Integer order) {
		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();
		if (order != null) {
			registry.setOrder(order);
		}
		return registry;
	}

	@Bean
	public HandlerMapping stompWebSocketHandlerMapping(
			WebSocketHandler subProtocolWebSocketHandler, TaskScheduler messageBrokerTaskScheduler,
			AbstractSubscribableChannel clientInboundChannel) {

		WebSocketHandler handler = decorateWebSocketHandler(subProtocolWebSocketHandler);
		WebMvcStompEndpointRegistry registry =
				new WebMvcStompEndpointRegistry(handler, getTransportRegistration(), messageBrokerTaskScheduler);
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null) {
			registry.setApplicationContext(applicationContext);
		}
		registerStompEndpoints(registry);
		OrderedMessageChannelDecorator.configureInterceptor(clientInboundChannel, registry.isPreserveReceiveOrder());
		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping instanceof WebSocketHandlerMapping webSocketMapping) {
			webSocketMapping.setPhase(getPhase());
		}
		return handlerMapping;
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		SubProtocolWebSocketHandler handler =
				new SubProtocolWebSocketHandler(clientInboundChannel, clientOutboundChannel);

		handler.setPhase(getPhase());
		return handler;
	}

	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		for (WebSocketHandlerDecoratorFactory factory : getTransportRegistration().getDecoratorFactories()) {
			handler = factory.decorate(handler);
		}
		return handler;
	}

	protected final WebSocketTransportRegistration getTransportRegistration() {
		if (this.transportRegistration == null) {
			this.transportRegistration = new WebSocketTransportRegistration();
			configureWebSocketTransport(this.transportRegistration);
		}
		return this.transportRegistration;
	}

	protected void configureWebSocketTransport(WebSocketTransportRegistration registry) {
	}

	protected abstract void registerStompEndpoints(StompEndpointRegistry registry);

	@Bean
	public static CustomScopeConfigurer webSocketScopeConfigurer() {
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("websocket", new SimpSessionScope());
		return configurer;
	}

	@Bean
	public WebSocketMessageBrokerStats webSocketMessageBrokerStats(
			@Nullable AbstractBrokerMessageHandler stompBrokerRelayMessageHandler,
			WebSocketHandler subProtocolWebSocketHandler,
			@Qualifier("clientInboundChannelExecutor") TaskExecutor inboundExecutor,
			@Qualifier("clientOutboundChannelExecutor") TaskExecutor outboundExecutor,
			@Qualifier("messageBrokerTaskScheduler") TaskScheduler scheduler) {

		WebSocketMessageBrokerStats stats = new WebSocketMessageBrokerStats();
		stats.setSubProtocolWebSocketHandler((SubProtocolWebSocketHandler) subProtocolWebSocketHandler);
		if (stompBrokerRelayMessageHandler instanceof StompBrokerRelayMessageHandler sbrmh) {
			stats.setStompBrokerRelay(sbrmh);
		}
		stats.setInboundChannelExecutor(inboundExecutor);
		stats.setOutboundChannelExecutor(outboundExecutor);
		stats.setSockJsTaskScheduler(scheduler);
		return stats;
	}

}
