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
