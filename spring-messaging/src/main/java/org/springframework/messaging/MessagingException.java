/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * The base exception for any failures related to messaging.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MessagingException extends NestedRuntimeException {

	private final @Nullable Message<?> failedMessage;


	public MessagingException(Message<?> message) {
		super(null, null);
		this.failedMessage = message;
	}

	public MessagingException(String description) {
		super(description);
		this.failedMessage = null;
	}

	public MessagingException(@Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
		this.failedMessage = null;
	}

	public MessagingException(Message<?> message, String description) {
		super(description);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, @Nullable Throwable cause) {
		super(null, cause);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, @Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
		this.failedMessage = message;
	}


	public @Nullable Message<?> getFailedMessage() {
		return this.failedMessage;
	}

	@Override
	public String toString() {
		return super.toString() + (this.failedMessage == null ? "" :
				(", failedMessage=" + this.failedMessage));
	}

}
