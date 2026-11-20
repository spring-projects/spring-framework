/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmssendingconversion;

import java.util.HashMap;
import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

public class JmsSenderWithConversion {

	private JmsTemplate jmsTemplate;

	public void sendWithConversion() {
		Map<String, Object> map = new HashMap<>();
		map.put("Name", "Mark");
		map.put("Age", 47);
		jmsTemplate.convertAndSend("testQueue", map, new MessagePostProcessor() {
			public Message postProcessMessage(Message message) throws JMSException {
				message.setIntProperty("AccountID", 1234);
				message.setJMSCorrelationID("123-00001");
				return message;
			}
		});
	}

}
