/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.websocketserverruntimeconfiguration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

// tag::snippet[]
@Configuration
class WebSocketConfiguration {

	@Bean
	fun createWebSocketContainer() = ServletServerContainerFactoryBean().apply {
		maxTextMessageBufferSize = 8192
		maxBinaryMessageBufferSize = 8192
	}
}
// end::snippet[]
