/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstomphandlesimplebroker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

// tag::snippet[]
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

	private lateinit var messageBrokerTaskScheduler: TaskScheduler

	@Autowired
	fun setMessageBrokerTaskScheduler(@Lazy taskScheduler: TaskScheduler) {
		this.messageBrokerTaskScheduler = taskScheduler
	}

	override fun configureMessageBroker(registry: MessageBrokerRegistry) {
		registry.enableSimpleBroker("/queue/", "/topic/")
			.setHeartbeatValue(longArrayOf(10000, 20000))
			.setTaskScheduler(messageBrokerTaskScheduler)

		// ...
	}
}
// end::snippet[]
