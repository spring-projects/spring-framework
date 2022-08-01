/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving
 * JMS attributes from/to generic message headers.
 *
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface JmsHeaders {

	/**
	 * Prefix used for JMS API related headers in order to distinguish from
	 * user-defined headers and other internal headers (e.g. correlationId).
	 * @see SimpleJmsHeaderMapper
	 */
	String PREFIX = "jms_";

	/**
	 * Correlation ID for the message. This may be the {@link #MESSAGE_ID} of
	 * the message that this message replies to. It may also be an
	 * application-specific identifier.
	 * @see jakarta.jms.Message#getJMSCorrelationID()
	 */
	String CORRELATION_ID = PREFIX + "correlationId";

	/**
	 * Name of the destination (topic or queue) of the message.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSDestination()
	 * @see jakarta.jms.Destination
	 * @see jakarta.jms.Queue
	 * @see jakarta.jms.Topic
	 */
	String DESTINATION = PREFIX + "destination";

	/**
	 * Distribution mode.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSDeliveryMode()
	 * @see jakarta.jms.DeliveryMode
	 */
	String DELIVERY_MODE = PREFIX + "deliveryMode";

	/**
	 * Message expiration date and time.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSExpiration()
	 */
	String EXPIRATION = PREFIX + "expiration";

	/**
	 * Unique identifier for a message.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSMessageID()
	 */
	String MESSAGE_ID = PREFIX + "messageId";

	/**
	 * The message priority level.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSPriority()
	 */
	String PRIORITY = PREFIX + "priority";

	/**
	 * Name of the destination (topic or queue) the message replies should
	 * be sent to.
	 * @see jakarta.jms.Message#getJMSReplyTo()
	 */
	String REPLY_TO = PREFIX + "replyTo";

	/**
	 * Specify if the message was resent. This occurs when a message
	 * consumer fails to acknowledge the message reception.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSRedelivered()
	 */
	String REDELIVERED = PREFIX + "redelivered";

	/**
	 * Message type label. This type is a string value describing the message
	 * in a functional manner.
	 * @see jakarta.jms.Message#getJMSType()
	 */
	String TYPE = PREFIX + "type";

	/**
	 * Date and time of the message sending operation.
	 * <p>Read-only value.
	 * @see jakarta.jms.Message#getJMSTimestamp()
	 */
	String TIMESTAMP = PREFIX + "timestamp";

}
