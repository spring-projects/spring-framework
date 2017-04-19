/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A {@link GenericMessage} with a {@link Throwable} payload.
 *
 * <p>The payload is typically a {@link org.springframework.messaging.MessagingException}
 * with the message at the point of failure in its {@code failedMessage} property.
 * An optional {@code originalMessage} may be provided, which represents the message
 * that existed at the point in the stack where the error message is created.
 *
 * <p>Consider some code that starts with a message, invokes some process that performs
 * transformation on that message and then fails for some reason, throwing the exception.
 * The exception is caught and an error message produced that contains both the original
 * message, and the transformed message that failed.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 4.0
 * @see MessageBuilder
 */
public class ErrorMessage extends GenericMessage<Throwable> {

	private static final long serialVersionUID = -5470210965279837728L;

	private final Message<?> originalMessage;


	/**
	 * Create a new message with the given payload.
	 * @param payload the message payload (never {@code null})
	 */
	public ErrorMessage(Throwable payload) {
		super(payload);
		this.originalMessage = null;
	}

	/**
	 * Create a new message with the given payload and headers.
	 * The content of the given header map is copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers to use for initialization
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers) {
		super(payload, headers);
		this.originalMessage = null;
	}

	/**
	 * A constructor with the {@link MessageHeaders} instance to use.
	 * <p><strong>Note:</strong> the given {@code MessageHeaders} instance
	 * is used directly in the new message, i.e. it is not copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers) {
		super(payload, headers);
		this.originalMessage = null;
	}

	/**
	 * Create a new message with the given payload and original message.
	 * @param payload the message payload (never {@code null})
	 * @param originalMessage the original message (if present) at the point
	 * in the stack where the ErrorMessage was created
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, Message<?> originalMessage) {
		super(payload);
		this.originalMessage = originalMessage;
	}

	/**
	 * Create a new message with the given payload, headers and original message.
	 * The content of the given header map is copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers to use for initialization
	 * @param originalMessage the original message (if present) at the point
	 * in the stack where the ErrorMessage was created
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}

	/**
	 * Create a new message with the payload, {@link MessageHeaders} and original message.
	 * <p><strong>Note:</strong> the given {@code MessageHeaders} instance
	 * is used directly in the new message, i.e. it is not copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers
	 * @param originalMessage the original message (if present) at the point
	 * in the stack where the ErrorMessage was created
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}


	/**
	 * Return the original message (if available) at the point in the stack
	 * where the ErrorMessage was created.
	 * @since 5.0
	 */
	public Message<?> getOriginalMessage() {
		return this.originalMessage;
	}

	@Override
	public String toString() {
		if (this.originalMessage == null) {
			return super.toString();
		}

		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(" for original ").append(this.originalMessage);
		return sb.toString();
	}

}
