/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.messaging;

import java.util.Map;

import javax.jms.Destination;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * A specialization of {@link MessageSendingOperations} for JMS related
 * operations that allows to specify a destination name rather than the
 * actual {@link javax.jms.Destination}
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.jms.core.JmsTemplate
 */
public interface JmsMessageSendingOperations extends MessageSendingOperations<Destination> {

	/**
	 * Send a message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param message the message to send
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message and send it to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 */
	void convertAndSend(String destinationName, Object payload) throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers and send it to
	 * the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers headers for the message to send
	 */
	void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message, apply the given post processor, and send
	 * the resulting message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post processor,
	 * and send the resulting message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers headers for the message to send
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, Map<String,
			Object> headers, MessagePostProcessor postProcessor) throws MessagingException;

}
