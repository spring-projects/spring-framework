/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import jakarta.jms.TextMessage;

// tag::snippet[]
public class DefaultResponsiveTextMessageDelegate implements ResponsiveTextMessageDelegate {

	@Override
	public String receive(TextMessage message) {
		return "message";
	}
}
// end::snippet[]
