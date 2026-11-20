/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstompserverconfig

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

	override fun configureWebSocketTransport(registry: WebSocketTransportRegistration) {
		registry.setMessageSizeLimit(4 * 8192)
		registry.setTimeToFirstMessage(30000)
	}
}
// end::snippet[]
