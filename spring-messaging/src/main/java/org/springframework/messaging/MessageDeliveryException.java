/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

import org.jspecify.annotations.Nullable;

/**
 * Exception that indicates an error occurred during message delivery.
 *
 * @author Mark Fisher
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MessageDeliveryException extends MessagingException {

	public MessageDeliveryException(String description) {
		super(description);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage) {
		super(undeliveredMessage);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage, String description) {
		super(undeliveredMessage, description);
	}

	public MessageDeliveryException(Message<?> message, @Nullable Throwable cause) {
		super(message, cause);
	}

	public MessageDeliveryException(Message<?> undeliveredMessage, String description, @Nullable Throwable cause) {
		super(undeliveredMessage, description, cause);
	}

}
