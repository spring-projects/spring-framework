/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.websocketserverhandler

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

// tag::snippet[]
class MyHandler : TextWebSocketHandler() {

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		// ...
	}
}
// end::snippet[]
