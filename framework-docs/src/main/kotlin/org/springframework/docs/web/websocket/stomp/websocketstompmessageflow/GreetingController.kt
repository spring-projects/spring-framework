package org.springframework.docs.web.websocket.stomp.websocketstompmessageflow

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.text.SimpleDateFormat
import java.util.*

// tag::snippet[]
@Controller
class GreetingController {
	
	@MessageMapping("/greeting")
	fun handle(greeting: String): String {
		return "[${getTimestamp()}: $greeting"
	}

	private fun getTimestamp(): String {
		return SimpleDateFormat("MM/dd/yyyy h:mm:ss a").format(Date())
	}
}
// end::snippet[]
