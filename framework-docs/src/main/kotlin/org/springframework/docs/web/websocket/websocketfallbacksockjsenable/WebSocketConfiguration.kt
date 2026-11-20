/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.websocketfallbacksockjsenable

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.docs.web.websocket.websocketserverhandler.MyHandler
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

// tag::snippet[]
@Configuration
@EnableWebSocket
class WebSocketConfiguration : WebSocketConfigurer {
	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(myHandler(), "/myHandler").withSockJS()
	}

	@Bean
	fun myHandler(): WebSocketHandler {
		return MyHandler()
	}
}
// end::snippet[]
