/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.core;

import org.springframework.messaging.Message;
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
 */
public abstract class AbstractMessageReceivingTemplate<D> extends AbstractMessageSendingTemplate<D>
		implements MessageReceivingOperations<D> {

	@Override
	public Message<?> receive() {
		return doReceive(getRequiredDefaultDestination());
	}

	@Override
	public Message<?> receive(D destination) {
		return doReceive(destination);
	}

	/**
	 * Actually receive a message from the given destination.
	 * @param destination the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	protected abstract Message<?> doReceive(D destination);


	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@Override
	public <T> T receiveAndConvert(D destination, Class<T> targetClass) {
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
	protected <T> T doConvert(Message<?> message, Class<T> targetClass) {
		MessageConverter messageConverter = getMessageConverter();
		T value = (T) messageConverter.fromMessage(message, targetClass);
		if (value == null) {
			throw new MessageConversionException(message, "Unable to convert payload [" + message.getPayload() +
					"] to type [" + targetClass + "] using converter [" + messageConverter + "]");
		}
		return value;
	}

}
