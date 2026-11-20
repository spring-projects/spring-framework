/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

/**
 * A generic message representation with headers and body.
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> the payload type
 * @see org.springframework.messaging.support.MessageBuilder
 */
public interface Message<T> {

	/**
	 * Return the message payload.
	 */
	T getPayload();

	/**
	 * Return message headers for the message (never {@code null} but may be empty).
	 */
	MessageHeaders getHeaders();

}
