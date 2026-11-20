/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

/**
 * Defines methods for sending messages.
 *
 * @author Mark Fisher
 * @since 4.0
 */
@FunctionalInterface
public interface MessageChannel {

	/**
	 * Constant for sending a message without a prescribed timeout.
	 */
	long INDEFINITE_TIMEOUT = -1;


	/**
	 * Send a {@link Message} to this channel. If the message is sent successfully,
	 * the method returns {@code true}. If the message cannot be sent due to a
	 * non-fatal reason, the method returns {@code false}. The method may also
	 * throw a RuntimeException in case of non-recoverable errors.
	 * <p>This method may block indefinitely, depending on the implementation.
	 * To provide a maximum wait time, use {@link #send(Message, long)}.
	 * @param message the message to send
	 * @return whether the message was sent
	 */
	default boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	/**
	 * Send a message, blocking until either the message is accepted or the
	 * specified timeout period elapses.
	 * @param message the message to send
	 * @param timeout the timeout in milliseconds or {@link #INDEFINITE_TIMEOUT}
	 * @return {@code true} if the message is sent, {@code false} if not
	 * including a timeout of an interrupt of the send
	 */
	boolean send(Message<?> message, long timeout);

}
