/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

/**
 * Simple contract for handling a {@link Message}.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @since 4.0
 * @see ReactiveMessageHandler
 */
@FunctionalInterface
public interface MessageHandler {

	/**
	 * Handle the given message.
	 * @param message the message to be handled
	 * @throws MessagingException if the handler failed to process the message
	 */
	void handleMessage(Message<?> message) throws MessagingException;

}
