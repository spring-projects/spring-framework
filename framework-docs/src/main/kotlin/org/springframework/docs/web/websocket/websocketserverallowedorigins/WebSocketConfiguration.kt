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
package org.springframework.docs.web.websocket.websocketserverallowedorigins

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
		registry.addHandler(myHandler(), "/myHandler").setAllowedOrigins("https://mydomain.com")
	}

	@Bean
	fun myHandler(): WebSocketHandler {
		return MyHandler()
	}
}
// end::snippet[]
