/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter

import java.io.Serializable

// tag::snippet[]
interface MessageDelegate {
	fun handleMessage(message: String)
	fun handleMessage(message: Map<*, *>)
	fun handleMessage(message: ByteArray)
	fun handleMessage(message: Serializable)
}
// end::snippet[]