/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import jakarta.jms.BytesMessage;
import jakarta.jms.MapMessage;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;

/**
 * See the MessageListenerAdapterTests class for usage.
 *
 * @author Rick Evans
 */
public interface MessageDelegate {

	void handleMessage(TextMessage message);

	void handleMessage(MapMessage message);

	void handleMessage(BytesMessage message);

	void handleMessage(ObjectMessage message);

}
