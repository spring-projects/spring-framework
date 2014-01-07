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

package org.springframework.web.socket.config.annotation;


import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import java.util.List;

/**
 * Defines methods for configuring message handling with simple messaging
 * protocols (e.g. STOMP) from WebSocket clients. Typically used to customize
 * the configuration provided via
 * {@link org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker @EnableWebSocketMessageBroker}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketMessageBrokerConfigurer {

	/**
	 * Configure STOMP over WebSocket endpoints.
	 */
	void registerStompEndpoints(StompEndpointRegistry registry);

	/**
	 * Configure the {@link org.springframework.messaging.MessageChannel} used for
	 * incoming messages from WebSocket clients. By default the channel is backed
	 * by a thread pool of size 1. It is recommended to customize thread pool
	 * settings for production use.
	 */
	void configureClientInboundChannel(ChannelRegistration registration);

	/**
	 * Configure the {@link org.springframework.messaging.MessageChannel} used for
	 * incoming messages from WebSocket clients. By default the channel is backed
	 * by a thread pool of size 1. It is recommended to customize thread pool
	 * settings for production use.
	 */
	void configureClientOutboundChannel(ChannelRegistration registration);

	/**
	 * Configure the message converters to use when extracting the payload of
	 * messages in annotated methods and when sending messages (e.g. through the
	 * "broker" SimpMessagingTemplate).
	 * <p>
	 * The provided list, initially empty, can be used to add message converters
	 * while the boolean return value is used to determine if default message should
	 * be added as well.
	 *
	 * @param messageConverters initially an empty list of converters
	 * @return whether to also add default converter or not
	 */
	boolean configureMessageConverters(List<MessageConverter> messageConverters);

	/**
	 * Configure message broker options.
	 */
	void configureMessageBroker(MessageBrokerRegistry registry);

}
