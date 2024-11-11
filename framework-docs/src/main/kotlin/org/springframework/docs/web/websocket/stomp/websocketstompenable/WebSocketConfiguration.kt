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

package org.springframework.docs.web.websocket.stomp.websocketstompenable

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		// /portfolio is the HTTP URL for the endpoint to which a WebSocket (or SockJS)
		// client needs to connect for the WebSocket handshake
		registry.addEndpoint("/portfolio")
	}

	override fun configureMessageBroker(config: MessageBrokerRegistry) {
		// STOMP messages whose destination header begins with /app are routed to
		// @MessageMapping methods in @Controller classes
		config.setApplicationDestinationPrefixes("/app")
		// Use the built-in message broker for subscriptions and broadcasting and
		// route messages whose destination header begins with /topic or /queue to the broker
		config.enableSimpleBroker("/topic", "/queue")
	}
}
// end::snippet[]