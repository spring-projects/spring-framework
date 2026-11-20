/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception raised by {@link MessageConverter} implementations.
 *
 * @author Mark Fisher
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MessageConversionException extends MessagingException {

	public MessageConversionException(String description) {
		super(description);
	}

	public MessageConversionException(@Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
	}

	public MessageConversionException(Message<?> failedMessage, String description) {
		super(failedMessage, description);
	}

	public MessageConversionException(Message<?> failedMessage, @Nullable String description, @Nullable Throwable cause) {
		super(failedMessage, description, cause);
	}

}
