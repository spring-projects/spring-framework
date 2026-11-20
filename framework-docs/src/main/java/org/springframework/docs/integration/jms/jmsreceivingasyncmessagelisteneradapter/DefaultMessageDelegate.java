/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import java.io.Serializable;
import java.util.Map;

// tag::snippet[]
public class DefaultMessageDelegate implements MessageDelegate {

	@Override
	public void handleMessage(String message) {
		// ...
	}

	@Override
	public void handleMessage(Map message) {
		// ...
	}

	@Override
	public void handleMessage(byte[] message) {
		// ...
	}

	@Override
	public void handleMessage(Serializable message) {
		// ...
	}
}
// end::snippet[]
