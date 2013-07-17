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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.messaging.support.converter.SimplePayloadMessageConverter;
import org.springframework.util.Assert;


/**
 * @author Mark Fisher
 * @since 4.0
 */
public abstract class AbstractMessageSendingTemplate<D> implements MessageSendingOperations<D> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile D defaultDestination;

	private volatile MessageConverter converter = new SimplePayloadMessageConverter();


	public void setDefaultDestination(D defaultDestination) {
		this.defaultDestination = defaultDestination;
	}

	public D getDefaultDestination() {
		return this.defaultDestination;
	}

	/**
	 * Set the {@link MessageConverter} that is to be used to convert
	 * between Messages and objects for this template.
	 * <p>The default is {@link SimplePayloadMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.converter = messageConverter;
	}

	/**
	 * @return the configured {@link MessageConverter}
	 */
	public MessageConverter getConverter() {
		return this.converter;
	}

	/**
	 * @param converter the converter to set
	 */
	public void setConverter(MessageConverter converter) {
		this.converter = converter;
	}

	@Override
	public <P> void send(Message<P> message) {
		this.send(getRequiredDefaultDestination(), message);
	}

	protected final D getRequiredDefaultDestination() {
		Assert.state(this.defaultDestination != null,
				"No 'defaultDestination' specified for MessagingTemplate. "
				+ "Unable to invoke method without an explicit destination argument.");
		return this.defaultDestination;
	}

	@Override
	public <P> void send(D destination, Message<P> message) {
		this.doSend(destination, message);
	}

	protected abstract void doSend(D destination, Message<?> message);


	@Override
	public <T> void convertAndSend(T message) {
		this.convertAndSend(getRequiredDefaultDestination(), message);
	}

	@Override
	public <T> void convertAndSend(D destination, T object) {
		this.convertAndSend(destination, object, null);
	}

	@Override
	public <T> void convertAndSend(T object, MessagePostProcessor postProcessor) {
		this.convertAndSend(getRequiredDefaultDestination(), object, postProcessor);
	}

	@Override
	public <T> void convertAndSend(D destination, T object, MessagePostProcessor postProcessor)
			throws MessagingException {

		@SuppressWarnings("unchecked")
		Message<?> message = this.converter.toMessage(object);
		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		this.send(destination, message);
	}

}
