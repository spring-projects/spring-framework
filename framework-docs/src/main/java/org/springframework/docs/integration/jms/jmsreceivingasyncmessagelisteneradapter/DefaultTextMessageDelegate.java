/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import org.springframework.web.socket.TextMessage;

// tag::snippet[]
public class DefaultTextMessageDelegate implements TextMessageDelegate {

	@Override
	public void receive(TextMessage message) {
		// ...
	}
}
// end::snippet[]
