/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.springframework.messaging.Message;

/**
 * A contract for processing a {@link Message} after it has been created, either
 * returning a modified (effectively new) message or returning the same.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see MessageSendingOperations
 * @see MessageRequestReplyOperations
 */
@FunctionalInterface
public interface MessagePostProcessor {

	/**
	 * Process the given message.
	 * @param message the message to process
	 * @return a post-processed variant of the message, or simply the incoming
	 * message; never {@code null}
	 */
	Message<?> postProcessMessage(Message<?> message);

}
