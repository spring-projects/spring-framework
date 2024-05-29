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

package org.springframework.docs.web.websocket.stomp.websocketstompserverconfig

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.time.Duration

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
class JettyWebSocketConfiguration : WebSocketMessageBrokerConfigurer {

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		registry.addEndpoint("/portfolio").setHandshakeHandler(handshakeHandler())
	}

	@Bean
	fun handshakeHandler(): DefaultHandshakeHandler {
		val strategy = JettyRequestUpgradeStrategy()
		strategy.addWebSocketConfigurer {
			it.inputBufferSize = 4 * 8192
			it.idleTimeout = Duration.ofSeconds(600)
		}
		return DefaultHandshakeHandler(strategy)
	}
}
// end::snippet[]
