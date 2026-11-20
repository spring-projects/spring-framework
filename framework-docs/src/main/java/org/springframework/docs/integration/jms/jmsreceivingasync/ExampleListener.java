/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasync;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

// tag::snippet[]
public class ExampleListener implements MessageListener {

	public void onMessage(Message message) {
		if (message instanceof TextMessage textMessage) {
			try {
				System.out.println(textMessage.getText());
			}
			catch (JMSException ex) {
				throw new RuntimeException(ex);
			}
		}
		else {
			throw new IllegalArgumentException("Message must be of type TextMessage");
		}
	}
}
// end::snippet[]
