/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jms.support.converter;

import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Convert a {@link Message} from the messaging abstraction to and from a
 * {@link jakarta.jms.Message} using an underlying {@link MessageConverter}
 * for the payload and a {@link org.springframework.jms.support.JmsHeaderMapper}
 * to map the JMS headers to and from standard message headers.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class MessagingMessageConverter implements MessageConverter, InitializingBean {

	private MessageConverter payloadConverter;

	private JmsHeaderMapper headerMapper;


	/**
	 * Create an instance with a default payload converter.
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 * @see org.springframework.jms.support.SimpleJmsHeaderMapper
	 */
	public MessagingMessageConverter() {
		this(new SimpleMessageConverter(), new SimpleJmsHeaderMapper());
	}

	/**
	 * Create an instance with the specific payload converter.
	 * @param payloadConverter the payload converter to use
	 * @since 4.3.12
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter) {
		this(payloadConverter, new SimpleJmsHeaderMapper());
	}

	/**
	 * Create an instance with the specified payload converter and
	 * header mapper.
	 */
	public MessagingMessageConverter(MessageConverter payloadConverter, JmsHeaderMapper headerMapper) {
		Assert.notNull(payloadConverter, "PayloadConverter must not be null");
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.payloadConverter = payloadConverter;
		this.headerMapper = headerMapper;
	}


	/**
	 * Set the {@link MessageConverter} to use to convert the payload.
	 */
	public void setPayloadConverter(MessageConverter payloadConverter) {
		this.payloadConverter = payloadConverter;
	}

	/**
	 * Set the {@link JmsHeaderMapper} to use to map JMS headers to and from
	 * standard message headers.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.payloadConverter, "Property 'payloadConverter' is required");
		Assert.notNull(this.headerMapper, "Property 'headerMapper' is required");
	}


	@Override
	public jakarta.jms.Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (!(object instanceof Message)) {
			throw new IllegalArgumentException("Could not convert [" + object + "] - only [" +
					Message.class.getName() + "] is handled by this converter");
		}
		Message<?> input = (Message<?>) object;
		MessageHeaders headers = input.getHeaders();
		Object conversionHint = headers.get(AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER);
		jakarta.jms.Message reply = createMessageForPayload(input.getPayload(), session, conversionHint);
		this.headerMapper.fromHeaders(headers, reply);
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object fromMessage(jakarta.jms.Message message) throws JMSException, MessageConversionException {
		Map<String, Object> mappedHeaders = extractHeaders(message);
		Object convertedObject = extractPayload(message);
		MessageBuilder<Object> builder = (convertedObject instanceof org.springframework.messaging.Message ?
				MessageBuilder.fromMessage((org.springframework.messaging.Message<Object>) convertedObject) :
				MessageBuilder.withPayload(convertedObject));
		return builder.copyHeadersIfAbsent(mappedHeaders).build();
	}

	/**
	 * Extract the payload of the specified {@link jakarta.jms.Message}.
	 */
	protected Object extractPayload(jakarta.jms.Message message) throws JMSException {
		return this.payloadConverter.fromMessage(message);
	}

	/**
	 * Create a JMS message for the specified payload and conversionHint.
	 * The conversion hint is an extra object passed to the {@link MessageConverter},
	 * e.g. the associated {@code MethodParameter} (may be {@code null}}.
	 * @since 4.3
	 * @see MessageConverter#toMessage(Object, Session)
	 */
	protected jakarta.jms.Message createMessageForPayload(
			Object payload, Session session, @Nullable Object conversionHint) throws JMSException {

		return this.payloadConverter.toMessage(payload, session);
	}

	protected final MessageHeaders extractHeaders(jakarta.jms.Message message) {
		return this.headerMapper.toHeaders(message);
	}

}
