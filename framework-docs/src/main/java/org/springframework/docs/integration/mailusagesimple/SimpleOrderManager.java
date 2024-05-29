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

package org.springframework.docs.integration.mailusagesimple;

import org.springframework.docs.integration.mailusage.OrderManager;
import org.springframework.mail.SimpleMailMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;

// tag::snippet[]
public class SimpleOrderManager implements OrderManager {

	private MailSender mailSender;
	private SimpleMailMessage templateMessage;

	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setTemplateMessage(SimpleMailMessage templateMessage) {
		this.templateMessage = templateMessage;
	}

	@Override
	public void placeOrder(Order order) {

		// Do the business calculations...

		// Call the collaborators to persist the order...

		// Create a thread-safe "copy" of the template message and customize it
		SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
		msg.setTo(order.getCustomer().getEmailAddress());
		msg.setText(
				"Dear " + order.getCustomer().getFirstName()
						+ order.getCustomer().getLastName()
						+ ", thank you for placing order. Your order number is "
						+ order.getOrderNumber());
		try {
			this.mailSender.send(msg);
		}
		catch (MailException ex) {
			// simply log it and go on...
			System.err.println(ex.getMessage());
		}
	}

}
// end::snippet[]