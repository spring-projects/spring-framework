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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.converter.ByteArrayMessageConverter;
import org.springframework.messaging.support.converter.CompositeMessageConverter;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.messaging.support.converter.StringMessageConverter;
import org.springframework.util.Assert;


/**
 * Base class for templates that support sending messages.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageSendingTemplate<D> implements MessageSendingOperations<D> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile D defaultDestination;

	private volatile MessageConverter converter;


	public AbstractMessageSendingTemplate() {
		Collection<MessageConverter> converters = new ArrayList<MessageConverter>();
		converters.add(new StringMessageConverter());
		converters.add(new ByteArrayMessageConverter());
		this.converter = new CompositeMessageConverter(converters);
	}

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
	public MessageConverter getMessageConverter() {
		return this.converter;
	}


	@Override
	public void send(Message<?> message) {
		this.send(getRequiredDefaultDestination(), message);
	}

	protected final D getRequiredDefaultDestination() {
		Assert.state(this.defaultDestination != null,
				"No 'defaultDestination' specified for MessagingTemplate. "
				+ "Unable to invoke method without an explicit destination argument.");
		return this.defaultDestination;
	}

	@Override
	public void send(D destination, Message<?> message) {
		this.doSend(destination, message);
	}

	protected abstract void doSend(D destination, Message<?> message);


	@Override
	public void convertAndSend(Object message) throws MessagingException {
		this.convertAndSend(getRequiredDefaultDestination(), message);
	}

	@Override
	public void convertAndSend(D destination, Object payload) throws MessagingException {
		this.convertAndSend(destination, payload, (Map<String, Object>) null);
	}

	@Override
	public void convertAndSend(D destination, Object payload, Map<String, Object> headers) throws MessagingException {
		MessagePostProcessor postProcessor = null;
		this.convertAndSend(destination, payload, headers, postProcessor);
	}

	@Override
	public void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException {
		this.convertAndSend(getRequiredDefaultDestination(), payload, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException {

		Map<String, Object> headers = null;
		this.convertAndSend(destination, payload, headers, postProcessor);
	}

	@Override
	public void convertAndSend(D destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {

		MessageHeaders messageHeaders = (headers != null) ? new MessageHeaders(headers) : null;
		Message<?> message = this.converter.toMessage(payload, messageHeaders);
		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		this.send(destination, message);
	}

}
