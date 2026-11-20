/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Operations for receiving messages from a destination.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> the type of destination
 * @see GenericMessagingTemplate
 */
public interface MessageReceivingOperations<D> {

	/**
	 * Receive a message from a default destination.
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable Message<?> receive() throws MessagingException;

	/**
	 * Receive a message from the given destination.
	 * @param destination the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable Message<?> receive(D destination) throws MessagingException;

	/**
	 * Receive a message from a default destination and convert its payload to the
	 * specified target class.
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> @Nullable T receiveAndConvert(Class<T> targetClass) throws MessagingException;

	/**
	 * Receive a message from the given destination and convert its payload to the
	 * specified target class.
	 * @param destination the target destination
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> @Nullable T receiveAndConvert(D destination, Class<T> targetClass) throws MessagingException;

}
