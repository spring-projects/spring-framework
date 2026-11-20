/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import java.io.Serializable;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.MapMessage;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;

/**
 * See the MessageListenerAdapterTests class for usage.
 *
 * @author Rick Evans
 */
public interface ResponsiveMessageDelegate {

	String handleMessage(TextMessage message);

	Map<String, Object> handleMessage(MapMessage message);

	byte[] handleMessage(BytesMessage message);

	Serializable handleMessage(ObjectMessage message);

}
