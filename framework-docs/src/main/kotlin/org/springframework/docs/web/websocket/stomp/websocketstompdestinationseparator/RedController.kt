/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

@file:Suppress("UNUSED_PARAMETER")
package org.springframework.docs.web.websocket.stomp.websocketstompdestinationseparator

import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

// tag::snippet[]
@Controller
@MessageMapping("red")
class RedController {

	@MessageMapping("blue.{green}")
	fun handleGreen(@DestinationVariable green: String) {
		// ...
	}
}
// end::snippet[]
