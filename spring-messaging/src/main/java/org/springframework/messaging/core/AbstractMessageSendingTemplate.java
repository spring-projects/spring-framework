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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract base class for implementations of {@link MessageSendingOperations}.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageSendingTemplate<D> implements MessageSendingOperations<D> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile D defaultDestination;

	private volatile MessageConverter converter = new SimpleMessageConverter();


	/**
	 * Configure the default destination to use in send methods that don't have
	 * a destination argument. If a default destination is not configured, send methods
	 * without a destination argument will raise an exception if invoked.
	 */
	public void setDefaultDestination(D defaultDestination) {
		this.defaultDestination = defaultDestination;
	}

	/**
	 * Return the configured default destination.
	 */
	public D getDefaultDestination() {
		return this.defaultDestination;
	}

	/**
	 * Set the {@link MessageConverter} to use in {@code convertAndSend} methods.
	 * <p>By default, {@link SimpleMessageConverter} is used.
	 * @param messageConverter the message converter to use
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.converter = messageConverter;
	}

	/**
	 * Return the configured {@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.converter;
	}


	@Override
	public void send(Message<?> message) {
		this.send(getRequiredDefaultDestination(), message);
	}

	protected final D getRequiredDefaultDestination() {
		Assert.state(this.defaultDestination != null, "No 'defaultDestination' configured.");
		return this.defaultDestination;
	}

	@Override
	public void send(D destination, Message<?> message) {
		this.doSend(destination, message);
	}

	protected abstract void doSend(D destination, Message<?> message);


	@Override
	public void convertAndSend(Object payload) throws MessagingException {
		this.convertAndSend(getRequiredDefaultDestination(), payload);
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

		headers = processHeadersToSend(headers);
		MessageHeaders messageHeaders = (headers != null) ? new MessageHeaders(headers) : null;
		Message<?> message = this.converter.toMessage(payload, messageHeaders);

		if (message == null) {
			String payloadType = (payload != null) ? payload.getClass().getName() : null;
			throw new MessageConversionException("Unable to convert payload type '"
					+ payloadType + "', Content-Type=" + messageHeaders.get(MessageHeaders.CONTENT_TYPE)
					+ ", converter=" + this.converter, null);
		}

		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		this.send(destination, message);
	}

	/**
	 * Provides access to the map of headers before a send operation.
	 * Implementations can modify the headers by returning a different map.
	 * This implementation returns the map that was passed in (i.e. without any changes).
	 *
	 * @param headers the headers to send, possibly {@code null}
	 * @return the actual headers to send
	 */
	protected Map<String, Object> processHeadersToSend(Map<String, Object> headers) {
		return headers;
	}

}
