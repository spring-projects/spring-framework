/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.mailusagesimple

import org.springframework.docs.integration.mailusage.OrderManager
import org.springframework.mail.MailException
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage

// tag::snippet[]
class SimpleOrderManager : OrderManager {

	lateinit var mailSender: MailSender
	lateinit var templateMessage: SimpleMailMessage

	override fun placeOrder(order: Order) {
		// Do the business calculations...

		// Call the collaborators to persist the order...

		// Create a thread-safe "copy" of the template message and customize it

		val msg = SimpleMailMessage(this.templateMessage)
		msg.setTo(order.customer.emailAddress)
		msg.text = ("Dear " + order.customer.firstName
				+ order.customer.lastName
				+ ", thank you for placing order. Your order number is "
				+ order.orderNumber)
		try {
			mailSender.send(msg)
		} catch (ex: MailException) {
			// simply log it and go on...
			System.err.println(ex.message)
		}
	}
}
// end::snippet[]