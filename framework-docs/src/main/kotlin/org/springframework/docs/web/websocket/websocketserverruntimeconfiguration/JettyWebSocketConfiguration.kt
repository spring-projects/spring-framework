/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.websocketserverruntimeconfiguration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.time.Duration

// tag::snippet[]
@Configuration
@EnableWebSocket
class JettyWebSocketConfiguration : WebSocketConfigurer {

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(echoWebSocketHandler(), "/echo").setHandshakeHandler(handshakeHandler())
	}

	@Bean
	fun echoWebSocketHandler(): WebSocketHandler {
		return MyEchoHandler()
	}

	@Bean
	fun handshakeHandler(): DefaultHandshakeHandler {
		val strategy = JettyRequestUpgradeStrategy()
		strategy.addWebSocketConfigurer {
			it.inputBufferSize = 8192
			it.idleTimeout = Duration.ofSeconds(600)
		}
		return DefaultHandshakeHandler(strategy)
	}
}
// end::snippet[]
