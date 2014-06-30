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

package org.springframework.web.socket.config.annotation;

import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpSessionScope;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * Extends {@link AbstractMessageBrokerConfiguration} and adds configuration for
 * receiving and responding to STOMP messages from WebSocket clients.
 * <p>
 * Typically used in conjunction with
 * {@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker} but can
 * also be extended directly.
 *
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @since 4.0
 */
public abstract class WebSocketMessageBrokerConfigurationSupport extends AbstractMessageBrokerConfiguration {

	// Check for setRemoveOnCancelPolicy method - available on JDK 7 and higher
	private static boolean hasRemoveOnCancelPolicyMethod = ClassUtils.hasMethod(
			ScheduledThreadPoolExecutor.class, "setRemoveOnCancelPolicy", boolean.class);

	private WebSocketTransportRegistration transportRegistration;


	protected WebSocketMessageBrokerConfigurationSupport() {
	}

	@Bean
	public HandlerMapping stompWebSocketHandlerMapping() {

		WebMvcStompEndpointRegistry registry = new WebMvcStompEndpointRegistry(subProtocolWebSocketHandler(),
				getTransportRegistration(), userSessionRegistry(), messageBrokerSockJsTaskScheduler());

		registry.setApplicationContext(getApplicationContext());
		registerStompEndpoints(registry);
		return registry.getHandlerMapping();
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler() {
		return new SubProtocolWebSocketHandler(clientInboundChannel(), clientOutboundChannel());
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

	/**
	 * The default TaskScheduler to use if none is configured via
	 * {@link SockJsServiceRegistration#setTaskScheduler(org.springframework.scheduling.TaskScheduler)}, i.e.
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
	public ThreadPoolTaskScheduler messageBrokerSockJsTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MessageBrokerSockJS-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		if (hasRemoveOnCancelPolicyMethod) {
			scheduler.setRemoveOnCancelPolicy(true);
		}
		return scheduler;
	}

	@Bean
	public static CustomScopeConfigurer webSocketScopeConfigurer() {
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.setScopes(Collections.<String, Object>singletonMap("websocket", new SimpSessionScope()));
		return configurer;
	}

}
