/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

@file:Suppress("UNUSED_VARIABLE")
package org.springframework.docs.web.websocket.stomp.websocketstompinterceptors

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor

// tag::snippet[]
class MyChannelInterceptor : ChannelInterceptor {

	override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
		val accessor = StompHeaderAccessor.wrap(message)
		val command = accessor.command
		// ...
		return message
	}
}
// end::snippet[]