/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.core;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An extension of {@link AbstractMessageReceivingTemplate} that adds support for
 * request-reply style operations as defined by {@link MessageRequestReplyOperations}.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.0
 * @param <D> the destination type
 */
public abstract class AbstractMessagingTemplate<D> extends AbstractMessageReceivingTemplate<D>
		implements MessageRequestReplyOperations<D> {

	@Override
	public @Nullable Message<?> sendAndReceive(Message<?> requestMessage) throws MessagingException {
		return sendAndReceive(getRequiredDefaultDestination(), requestMessage);
	}

	@Override
	public @Nullable Message<?> sendAndReceive(D destination, Message<?> requestMessage) throws MessagingException {
		return doSendAndReceive(destination, requestMessage);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(Object request, Class<T> targetClass) throws MessagingException {
		return convertSendAndReceive(request, null, targetClass, null);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(D destination, Object request, Class<T> targetClass) throws MessagingException {
		return convertSendAndReceive(destination, request, null, targetClass, null);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(Object request, @Nullable Map<String, Object> headers, Class<T> targetClass) throws MessagingException {
		return convertSendAndReceive(request, headers, targetClass, null);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(
			D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException {

		return convertSendAndReceive(destination, request, headers, targetClass, null);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(
			Object request, Class<T> targetClass, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		return convertSendAndReceive(request, null, targetClass, postProcessor);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		return convertSendAndReceive(destination, request, null, targetClass, postProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T convertSendAndReceive(Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) throws MessagingException {

		return convertSendAndReceive(getRequiredDefaultDestination(), request, headers, targetClass, postProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T convertSendAndReceive(D destination, Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) throws MessagingException {

		Message<?> requestMessage = doConvert(request, headers, postProcessor);
		Message<?> replyMessage = doSendAndReceive(destination, requestMessage);
		return (replyMessage != null ? (T) getMessageConverter().fromMessage(replyMessage, targetClass) : null);
	}


	/**
	 * Actually send the given request message to the given destination and
	 * receive a reply message for it.
	 * @param destination the target destination
	 * @param requestMessage the message to send
	 * @return the received reply, possibly {@code null} if the
	 * message could not be received, for example due to a timeout
	 */
	protected abstract @Nullable Message<?> doSendAndReceive(D destination, Message<?> requestMessage);

}
