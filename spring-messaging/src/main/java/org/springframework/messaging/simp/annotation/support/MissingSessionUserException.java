/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.annotation.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * {@link MessagingException} thrown when a session is missing.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MissingSessionUserException extends MessagingException {

	public MissingSessionUserException(Message<?> message) {
		super(message, "No \"user\" header in message");
	}

}
