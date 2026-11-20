/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter

import jakarta.jms.TextMessage

// tag::snippet[]
class DefaultResponsiveTextMessageDelegate : ResponsiveTextMessageDelegate {

	override fun receive(message: TextMessage): String {
		return "message"
	}
}
// end::snippet[]