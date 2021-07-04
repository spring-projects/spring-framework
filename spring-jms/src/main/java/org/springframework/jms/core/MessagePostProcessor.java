/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * To be used with JmsTemplate's send method that converts an object to a message.
 *
 * <p>This allows for further modification of the message after it has been processed
 * by the converter and is useful for setting JMS headers and properties.
 *
 * <p>Often implemented as a lambda expression or as an anonymous inner class.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see JmsTemplate#convertAndSend(String, Object, MessagePostProcessor)
 * @see JmsTemplate#convertAndSend(javax.jms.Destination, Object, MessagePostProcessor)
 * @see org.springframework.jms.support.converter.MessageConverter
 */
@FunctionalInterface
public interface MessagePostProcessor {

	/**
	 * Process the given message.
	 * <p>The returned message is typically a modified version of the original.
	 * @param message the JMS message from the MessageConverter
	 * @return a post-processed variant of the message, or simply the incoming
	 * message; never {@code null}
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	Message postProcessMessage(Message message) throws JMSException;

}
