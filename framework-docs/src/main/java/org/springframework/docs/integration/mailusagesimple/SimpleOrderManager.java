/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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