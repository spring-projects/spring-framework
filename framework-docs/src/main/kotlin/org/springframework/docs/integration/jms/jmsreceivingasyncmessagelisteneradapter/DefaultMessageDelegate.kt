/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter

import java.io.Serializable

// tag::snippet[]
class DefaultMessageDelegate : MessageDelegate {

	override fun handleMessage(message: String) {
		// ...
	}

	override fun handleMessage(message: Map<*, *>) {
		// ...
	}

	override fun handleMessage(message: ByteArray) {
		// ...
	}

	override fun handleMessage(message: Serializable) {
		// ...
	}
}
// end::snippet[]