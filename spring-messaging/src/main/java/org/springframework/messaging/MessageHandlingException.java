/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

/**
 * Exception that indicates an error occurred during message handling.
 *
 * @author Mark Fisher
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MessageHandlingException extends MessagingException {

	public MessageHandlingException(Message<?> failedMessage) {
		super(failedMessage);
	}

	public MessageHandlingException(Message<?> message, String description) {
		super(message, description);
	}

	public MessageHandlingException(Message<?> failedMessage, Throwable cause) {
		super(failedMessage, cause);
	}

	public MessageHandlingException(Message<?> message, String description, Throwable cause) {
		super(message, description, cause);
	}

}
