/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An extension of {@link AbstractMessageReceivingTemplate} that adds support for
 * request-reply style operations as defined by {@link MessageRequestReplyOperations}.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 * @param <D> the destination type
 */
public abstract class AbstractMessagingTemplate<D> extends AbstractMessageReceivingTemplate<D>
		implements MessageRequestReplyOperations<D> {

	@Override
	@Nullable
	public Message<?> sendAndReceive(Message<?> requestMessage) {
		return sendAndReceive(getRequiredDefaultDestination(), requestMessage);
	}

	@Override
	@Nullable
	public Message<?> sendAndReceive(D destination, Message<?> requestMessage) {
		return doSendAndReceive(destination, requestMessage);
	}

	@Nullable
	protected abstract Message<?> doSendAndReceive(D destination, Message<?> requestMessage);


	@Override
	@Nullable
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass) {
		return convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass) {
		return convertSendAndReceive(destination, request, null, targetClass);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(
			D destination, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass) {

		return convertSendAndReceive(destination, request, headers, targetClass, null);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(
			Object request, Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) {

		return convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass, postProcessor);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) {

		return convertSendAndReceive(destination, request, null, targetClass, postProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T convertSendAndReceive(D destination, Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) {

		Message<?> requestMessage = doConvert(request, headers, postProcessor);
		Message<?> replyMessage = sendAndReceive(destination, requestMessage);
		return (replyMessage != null ? (T) getMessageConverter().fromMessage(replyMessage, targetClass) : null);
	}

}
