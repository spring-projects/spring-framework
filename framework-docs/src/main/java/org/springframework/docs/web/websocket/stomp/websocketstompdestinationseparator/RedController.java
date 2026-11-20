/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.websocket.stomp.websocketstompdestinationseparator;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

// tag::snippet[]
@Controller
@MessageMapping("red")
public class RedController {

	@MessageMapping("blue.{green}")
	public void handleGreen(@DestinationVariable String green) {
		// ...
	}
}
// end::snippet[]
