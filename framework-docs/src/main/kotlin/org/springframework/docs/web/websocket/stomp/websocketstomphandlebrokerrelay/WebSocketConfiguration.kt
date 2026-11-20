/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstomphandlebrokerrelay

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
		registry.addEndpoint("/portfolio").withSockJS()
	}

	override fun configureMessageBroker(registry: MessageBrokerRegistry) {
		registry.enableStompBrokerRelay("/topic", "/queue")
		registry.setApplicationDestinationPrefixes("/app")
	}
}
// end::snippet[]