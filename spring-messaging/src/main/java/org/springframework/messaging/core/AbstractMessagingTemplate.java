/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Base class for a messaging template that send and receive messages.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessagingTemplate<D> extends AbstractMessageSendingTemplate<D>
		implements MessageRequestReplyOperations<D>, MessageReceivingOperations<D> {


	@Override
	public <P> Message<P> receive() {
		return this.receive(getRequiredDefaultDestination());
	}

	@Override
	public <P> Message<P> receive(D destination) {
		return this.doReceive(destination);
	}

	protected abstract <P> Message<P> doReceive(D destination);


	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) {
		return this.receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T receiveAndConvert(D destination, Class<T> targetClass) {
		Message<?> message = this.doReceive(destination);
		if (message != null) {
			return (T) getMessageConverter().fromMessage(message, targetClass);
		}
		else {
			return null;
		}
	}

	@Override
	public Message<?> sendAndReceive(Message<?> requestMessage) {
		return this.sendAndReceive(getRequiredDefaultDestination(), requestMessage);
	}

	@Override
	public Message<?> sendAndReceive(D destination, Message<?> requestMessage) {
		return this.doSendAndReceive(destination, requestMessage);
	}

	protected abstract <S, R> Message<R> doSendAndReceive(D destination, Message<S> requestMessage);


	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass) {
		return this.convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass) {
		Map<String, Object> headers = null;
		return this.convertSendAndReceive(destination, request, headers, targetClass);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers,
			Class<T> targetClass) {

		MessagePostProcessor postProcessor = null;
		return this.convertSendAndReceive(destination, request, headers, targetClass, postProcessor);
	}

	@Override
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass, MessagePostProcessor postProcessor) {
		return this.convertSendAndReceive(getRequiredDefaultDestination(), request, targetClass, postProcessor);
	}

	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
			MessagePostProcessor postProcessor) {

		Map<String, Object> headers = null;
		return this.convertSendAndReceive(destination, request, headers, targetClass, postProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor postProcessor) {

		MessageHeaders messageHeaders = (headers != null) ? new MessageHeaders(headers) : null;
		Message<?> requestMessage = getMessageConverter().toMessage(request, messageHeaders);
		if (postProcessor != null) {
			requestMessage = postProcessor.postProcessMessage(requestMessage);
		}
		Message<?> replyMessage = this.sendAndReceive(destination, requestMessage);
		return (T) getMessageConverter().fromMessage(replyMessage, targetClass);
	}

}
