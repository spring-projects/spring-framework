/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * An extension of {@link AbstractMessageSendingTemplate} that adds support for
 * receive style operations as defined by {@link MessageReceivingOperations}.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.1
 * @param <D> the destination type
 */
public abstract class AbstractMessageReceivingTemplate<D> extends AbstractMessageSendingTemplate<D>
		implements MessageReceivingOperations<D> {

	@Override
	public @Nullable Message<?> receive() throws MessagingException {
		return doReceive(getRequiredDefaultDestination());
	}

	@Override
	public @Nullable Message<?> receive(D destination) throws MessagingException {
		return doReceive(destination);
	}

	@Override
	public <T> @Nullable T receiveAndConvert(Class<T> targetClass) throws MessagingException {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@Override
	public <T> @Nullable T receiveAndConvert(D destination, Class<T> targetClass) throws MessagingException {
		Message<?> message = doReceive(destination);
		if (message != null) {
			return doConvert(message, targetClass);
		}
		else {
			return null;
		}
	}

	/**
	 * Convert from the given message to the given target class.
	 * @param message the message to convert
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message (never {@code null})
	 */
	@SuppressWarnings("unchecked")
	protected <T> @Nullable T doConvert(Message<?> message, Class<T> targetClass) {
		MessageConverter messageConverter = getMessageConverter();
		T value = (T) messageConverter.fromMessage(message, targetClass);
		if (value == null) {
			throw new MessageConversionException(message, "Unable to convert payload [" + message.getPayload() +
					"] to type [" + targetClass + "] using converter [" + messageConverter + "]");
		}
		return value;
	}

	/**
	 * Actually receive a message from the given destination.
	 * @param destination the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	protected abstract @Nullable Message<?> doReceive(D destination);

}
