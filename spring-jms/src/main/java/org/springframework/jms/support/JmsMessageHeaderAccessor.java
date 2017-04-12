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

package org.springframework.jms.support;

import java.util.List;
import java.util.Map;
import javax.jms.Destination;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

/**
 * A {@link org.springframework.messaging.support.MessageHeaderAccessor}
 * implementation giving access to JMS-specific headers.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class JmsMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	protected JmsMessageHeaderAccessor(Map<String, List<String>> nativeHeaders) {
		super(nativeHeaders);
	}

	protected JmsMessageHeaderAccessor(Message<?> message) {
		super(message);
	}


	/**
	 * Return the {@link JmsHeaders#CORRELATION_ID correlationId}.
	 * @see JmsHeaders#CORRELATION_ID
	 */
	public String getCorrelationId() {
		return (String) getHeader(JmsHeaders.CORRELATION_ID);
	}

	/**
	 * Return the {@link JmsHeaders#DESTINATION destination}.
	 * @see JmsHeaders#DESTINATION
	 */
	public Destination getDestination() {
		return (Destination) getHeader(JmsHeaders.DESTINATION);
	}

	/**
	 * Return the {@link JmsHeaders#DELIVERY_MODE delivery mode}.
	 * @see JmsHeaders#DELIVERY_MODE
	 */
	public Integer getDeliveryMode() {
		return (Integer) getHeader(JmsHeaders.DELIVERY_MODE);
	}

	/**
	 * Return the message {@link JmsHeaders#EXPIRATION expiration}.
	 * @see JmsHeaders#EXPIRATION
	 */
	public Long getExpiration() {
		return (Long) getHeader(JmsHeaders.EXPIRATION);
	}

	/**
	 * Return the {@link JmsHeaders#MESSAGE_ID message id}.
	 * @see JmsHeaders#MESSAGE_ID
	 */
	public String getMessageId() {
		return (String) getHeader(JmsHeaders.MESSAGE_ID);
	}

	/**
	 * Return the {@link JmsHeaders#PRIORITY}.
	 * @see JmsHeaders#PRIORITY
	 */
	public Integer getPriority() {
		return (Integer) getHeader(JmsHeaders.PRIORITY);
	}

	/**
	 * Return the {@link JmsHeaders#REPLY_TO reply to}.
	 * @see JmsHeaders#REPLY_TO
	 */
	public Destination getReplyTo() {
		return (Destination) getHeader(JmsHeaders.REPLY_TO);
	}

	/**
	 * Return the {@link JmsHeaders#REDELIVERED redelivered} flag.
	 * @see JmsHeaders#REDELIVERED
	 */
	public Boolean getRedelivered() {
		return (Boolean) getHeader(JmsHeaders.REDELIVERED);
	}

	/**
	 * Return the {@link JmsHeaders#TYPE type}.
	 * @see JmsHeaders#TYPE
	 */
	public String getType() {
		return (String) getHeader(JmsHeaders.TYPE);
	}

	/**
	 * Return the {@link JmsHeaders#TIMESTAMP timestamp}.
	 * @see JmsHeaders#TIMESTAMP
	 */
	public Long getTimestamp() {
		return (Long) getHeader(JmsHeaders.TIMESTAMP);
	}


	// Static factory method

	/**
	 * Create a {@link JmsMessageHeaderAccessor} from the headers of an existing message.
	 */
	public static JmsMessageHeaderAccessor wrap(Message<?> message) {
		return new JmsMessageHeaderAccessor(message);
	}

}
