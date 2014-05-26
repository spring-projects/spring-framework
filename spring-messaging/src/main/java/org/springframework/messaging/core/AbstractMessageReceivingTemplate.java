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
		return receive(getRequiredDefaultDestination());
	}

	@Override
	public Message<?> receive(D destination) {
		return doReceive(destination);
	}

	protected abstract Message<?> doReceive(D destination);


	@Override
	public <T> T receiveAndConvert(Class<T> targetClass) {
		return receiveAndConvert(getRequiredDefaultDestination(), targetClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T receiveAndConvert(D destination, Class<T> targetClass) {
		Message<?> message = doReceive(destination);
		if (message != null) {
			return (T) getMessageConverter().fromMessage(message, targetClass);
		}
		else {
			return null;
		}
	}

}
