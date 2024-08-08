/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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