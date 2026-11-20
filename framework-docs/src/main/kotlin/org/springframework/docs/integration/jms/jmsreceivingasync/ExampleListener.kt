/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasync

import jakarta.jms.JMSException
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage

// tag::snippet[]
class ExampleListener : MessageListener {

	override fun onMessage(message: Message) {
		if (message is TextMessage) {
			try {
				println(message.text)
			} catch (ex: JMSException) {
				throw RuntimeException(ex)
			}
		} else {
			throw IllegalArgumentException("Message must be of type TextMessage")
		}
	}
}
// end::snippet[]