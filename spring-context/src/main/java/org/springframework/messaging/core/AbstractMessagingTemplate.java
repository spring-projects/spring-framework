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

import org.springframework.messaging.Message;


/**
 * @author Mark Fisher
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
	public Object receiveAndConvert() {
		return this.receiveAndConvert(getRequiredDefaultDestination());
	}

	@Override
	public Object receiveAndConvert(D destination) {
		Message<?> message = this.doReceive(destination);
		return (message != null) ? this.converter.fromMessage(message, null) : null;
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
	public Object convertSendAndReceive(Object request) {
		return this.convertSendAndReceive(getRequiredDefaultDestination(), request);
	}

	@Override
	public Object convertSendAndReceive(D destination, Object request) {
		return this.convertSendAndReceive(destination, request, null);
	}

	@Override
	public Object convertSendAndReceive(Object request, MessagePostProcessor postProcessor) {
		return this.convertSendAndReceive(getRequiredDefaultDestination(), request, postProcessor);
	}

	@Override
	public Object convertSendAndReceive(D destination, Object request, MessagePostProcessor postProcessor) {
		Message<?> requestMessage = this.converter.toMessage(request);
		if (postProcessor != null) {
			requestMessage = postProcessor.postProcessMessage(requestMessage);
		}
		Message<?> replyMessage = this.sendAndReceive(destination, requestMessage);
		return this.converter.fromMessage(replyMessage, null);
	}

}
