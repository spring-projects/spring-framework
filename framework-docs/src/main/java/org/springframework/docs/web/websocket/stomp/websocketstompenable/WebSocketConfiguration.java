/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstompenable;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// /portfolio is the HTTP URL for the endpoint to which a WebSocket (or SockJS)
		// client needs to connect for the WebSocket handshake
		registry.addEndpoint("/portfolio");
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// STOMP messages whose destination header begins with /app are routed to
		// @MessageMapping methods in @Controller classes
		config.setApplicationDestinationPrefixes("/app");
		// Use the built-in message broker for subscriptions and broadcasting and
		// route messages whose destination header begins with /topic or /queue to the broker
		config.enableSimpleBroker("/topic", "/queue");
	}
}
// end::snippet[]
