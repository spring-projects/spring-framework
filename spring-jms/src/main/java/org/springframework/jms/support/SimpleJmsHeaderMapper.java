/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jms.support;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.AbstractHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * Simple implementation of {@link JmsHeaderMapper}.
 *
 * <p>This implementation copies JMS API headers (for example, JMSReplyTo) to and from
 * {@link org.springframework.messaging.Message Messages}. Any user-defined
 * properties will also be copied from a JMS Message to a Message, and any
 * other headers on a Message (beyond the JMS API headers) will likewise
 * be copied to a JMS Message. Those other headers will be copied to the
 * general properties of a JMS Message whereas the JMS API headers are passed
 * to the appropriate setter methods (for example, setJMSReplyTo).
 *
 * <p>Constants for the JMS API headers are defined in {@link JmsHeaders}.
 * Note that most of the JMS headers are read-only: the JMSDestination,
 * JMSDeliveryMode, JMSExpiration, JMSMessageID, JMSPriority, JMSRedelivered
 * and JMSTimestamp flags are only copied <em>from</em> a JMS Message. Those
 * values will <em>not</em> be passed along from a Message to an outbound
 * JMS Message.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 4.1
 */
public class SimpleJmsHeaderMapper extends AbstractHeaderMapper<Message> implements JmsHeaderMapper {

	private static final Set<Class<?>> SUPPORTED_PROPERTY_TYPES = Set.of(Boolean.class, Byte.class,
			Double.class, Float.class, Integer.class, Long.class, Short.class, String.class, byte[].class);


	@Override
	public void fromHeaders(MessageHeaders headers, jakarta.jms.Message jmsMessage) {
		try {
			Object jmsCorrelationId = headers.get(JmsHeaders.CORRELATION_ID);
			if (jmsCorrelationId instanceof Number) {
				jmsCorrelationId = jmsCorrelationId.toString();
			}
			if (jmsCorrelationId instanceof String correlationId) {
				try {
					jmsMessage.setJMSCorrelationID(correlationId);
				}
				catch (Exception ex) {
					logger.debug("Failed to set JMSCorrelationID - skipping", ex);
				}
			}
			Destination jmsReplyTo = getHeaderIfAvailable(headers, JmsHeaders.REPLY_TO, Destination.class);
			if (jmsReplyTo != null) {
				try {
					jmsMessage.setJMSReplyTo(jmsReplyTo);
				}
				catch (Exception ex) {
					logger.debug("Failed to set JMSReplyTo - skipping", ex);
				}
			}
			String jmsType = getHeaderIfAvailable(headers, JmsHeaders.TYPE, String.class);
			if (jmsType != null) {
				try {
					jmsMessage.setJMSType(jmsType);
				}
				catch (Exception ex) {
					logger.debug("Failed to set JMSType - skipping", ex);
				}
			}
			for (Map.Entry<String, Object> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				if (StringUtils.hasText(headerName) && !headerName.startsWith(JmsHeaders.PREFIX)) {
					Object value = entry.getValue();
					if (value != null && SUPPORTED_PROPERTY_TYPES.contains(value.getClass())) {
						try {
							String propertyName = fromHeaderName(headerName);
							jmsMessage.setObjectProperty(propertyName, value);
						}
						catch (Exception ex) {
							if (headerName.startsWith("JMSX")) {
								if (logger.isTraceEnabled()) {
									logger.trace("Skipping reserved header '" + headerName +
											"' since it cannot be set by client");
								}
							}
							else if (logger.isDebugEnabled()) {
								logger.debug("Failed to map message header '" + headerName + "' to JMS property", ex);
							}
						}
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error occurred while mapping from MessageHeaders to JMS properties", ex);
			}
		}
	}

	@Override
	public MessageHeaders toHeaders(jakarta.jms.Message jmsMessage) {
		Map<String, Object> headers = new HashMap<>();
		try {
			try {
				String correlationId = jmsMessage.getJMSCorrelationID();
				if (correlationId != null) {
					headers.put(JmsHeaders.CORRELATION_ID, correlationId);
				}
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSCorrelationID property - skipping", ex);
			}
			try {
				Destination destination = jmsMessage.getJMSDestination();
				if (destination != null) {
					headers.put(JmsHeaders.DESTINATION, destination);
				}
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSDestination property - skipping", ex);
			}
			try {
				int deliveryMode = jmsMessage.getJMSDeliveryMode();
				headers.put(JmsHeaders.DELIVERY_MODE, deliveryMode);
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSDeliveryMode property - skipping", ex);
			}
			try {
				long expiration = jmsMessage.getJMSExpiration();
				headers.put(JmsHeaders.EXPIRATION, expiration);
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSExpiration property - skipping", ex);
			}
			try {
				String messageId = jmsMessage.getJMSMessageID();
				if (messageId != null) {
					headers.put(JmsHeaders.MESSAGE_ID, messageId);
				}
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSMessageID property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.PRIORITY, jmsMessage.getJMSPriority());
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSPriority property - skipping", ex);
			}
			try {
				Destination replyTo = jmsMessage.getJMSReplyTo();
				if (replyTo != null) {
					headers.put(JmsHeaders.REPLY_TO, replyTo);
				}
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSReplyTo property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.REDELIVERED, jmsMessage.getJMSRedelivered());
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSRedelivered property - skipping", ex);
			}
			try {
				String type = jmsMessage.getJMSType();
				if (type != null) {
					headers.put(JmsHeaders.TYPE, type);
				}
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSType property - skipping", ex);
			}
			try {
				headers.put(JmsHeaders.TIMESTAMP, jmsMessage.getJMSTimestamp());
			}
			catch (Exception ex) {
				logger.debug("Failed to read JMSTimestamp property - skipping", ex);
			}

			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					try {
						String headerName = toHeaderName(propertyName);
						headers.put(headerName, jmsMessage.getObjectProperty(propertyName));
					}
					catch (Exception ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Error occurred while mapping JMS property '" + propertyName +
									"' to Message header", ex);
						}
					}
				}
			}
		}
		catch (JMSException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error occurred while mapping from JMS properties to MessageHeaders", ex);
			}
		}
		return new MessageHeaders(headers);
	}

	/**
	 * Add the outbound prefix if necessary.
	 * <p>Convert {@link MessageHeaders#CONTENT_TYPE} to {@code content_type} for JMS compliance.
	 * @see #CONTENT_TYPE_PROPERTY
	 */
	@Override
	protected String fromHeaderName(String headerName) {
		if (MessageHeaders.CONTENT_TYPE.equals(headerName)) {
			return CONTENT_TYPE_PROPERTY;
		}
		return super.fromHeaderName(headerName);
	}

	/**
	 * Add the inbound prefix if necessary.
	 * <p>Convert the JMS-compliant {@code content_type} to {@link MessageHeaders#CONTENT_TYPE}.
	 * @see #CONTENT_TYPE_PROPERTY
	 */
	@Override
	protected String toHeaderName(String propertyName) {
		if (CONTENT_TYPE_PROPERTY.equals(propertyName)) {
			return MessageHeaders.CONTENT_TYPE;
		}
		return super.toHeaderName(propertyName);
	}

}
