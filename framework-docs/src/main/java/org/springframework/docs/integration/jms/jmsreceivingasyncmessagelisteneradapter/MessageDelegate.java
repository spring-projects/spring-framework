/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import java.io.Serializable;
import java.util.Map;

// tag::snippet[]
public interface MessageDelegate {

	void handleMessage(String message);

	void handleMessage(Map message);

	void handleMessage(byte[] message);

	void handleMessage(Serializable message);
}
// end::snippet[]
