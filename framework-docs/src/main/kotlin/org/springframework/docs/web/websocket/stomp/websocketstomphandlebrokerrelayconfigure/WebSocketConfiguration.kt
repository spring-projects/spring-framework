/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

@file:Suppress("DEPRECATION")
package org.springframework.docs.web.websocket.stomp.websocketstomphandlebrokerrelayconfigure

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.net.InetSocketAddress

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

	// ...

	override fun configureMessageBroker(registry: MessageBrokerRegistry) {
		registry.enableStompBrokerRelay("/queue/", "/topic/").setTcpClient(createTcpClient())
		registry.setApplicationDestinationPrefixes("/app")
	}

	private fun createTcpClient(): ReactorNettyTcpClient<ByteArray> {
		return ReactorNettyTcpClient({ it.addressSupplier { InetSocketAddress(0) } }, StompReactorNettyCodec())
	}
}
// end::snippet[]