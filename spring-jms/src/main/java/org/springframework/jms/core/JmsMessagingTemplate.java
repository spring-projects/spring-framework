/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.core;

import java.util.Map;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.core.AbstractMessagingTemplate;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * An implementation of {@link JmsMessageOperations}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class JmsMessagingTemplate extends AbstractMessagingTemplate<Destination>
		implements JmsMessageOperations, InitializingBean {

	@Nullable
	private JmsTemplate jmsTemplate;

	private MessageConverter jmsMessageConverter = new MessagingMessageConverter();

	private boolean converterSet;

	@Nullable
	private String defaultDestinationName;


	/**
	 * Constructor for use with bean properties.
	 * Requires {@link #setConnectionFactory} or {@link #setJmsTemplate} to be called.
	 */
	public JmsMessagingTemplate() {
	}

	/**
	 * Create a {@code JmsMessagingTemplate} instance with the JMS {@link ConnectionFactory}
	 * to use, implicitly building a {@link JmsTemplate} based on it.
	 * @since 4.1.2
	 */
	public JmsMessagingTemplate(ConnectionFactory connectionFactory) {
		this.jmsTemplate = new JmsTemplate(connectionFactory);
	}

	/**
	 * Create a {@code JmsMessagingTemplate} instance with the {@link JmsTemplate} to use.
	 */
	public JmsMessagingTemplate(JmsTemplate jmsTemplate) {
		Assert.notNull(jmsTemplate, "JmsTemplate must not be null");
		this.jmsTemplate = jmsTemplate;
	}


	/**
	 * Set the ConnectionFactory to use for the underlying {@link JmsTemplate}.
	 * @since 4.1.2
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		if (this.jmsTemplate != null) {
			this.jmsTemplate.setConnectionFactory(connectionFactory);
		}
		else {
			this.jmsTemplate = new JmsTemplate(connectionFactory);
		}
	}

	/**
	 * Return the ConnectionFactory that the underlying {@link JmsTemplate} uses.
	 * @since 4.1.2
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return (this.jmsTemplate != null ? this.jmsTemplate.getConnectionFactory() : null);
	}

	/**
	 * Set the {@link JmsTemplate} to use.
	 */
	public void setJmsTemplate(@Nullable JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * Return the configured {@link JmsTemplate}.
	 */
	@Nullable
	public JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	/**
	 * Set the {@link MessageConverter} to use to convert a {@link Message} from
	 * the messaging to and from a {@link jakarta.jms.Message}. By default, a
	 * {@link MessagingMessageConverter} is defined using a {@link SimpleMessageConverter}
	 * to convert the payload of the message.
	 * <p>Consider configuring a {@link MessagingMessageConverter} with a different
	 * {@link MessagingMessageConverter#setPayloadConverter(MessageConverter) payload converter}
	 * for more advanced scenarios.
	 * @see org.springframework.jms.support.converter.MessagingMessageConverter
	 */
	public void setJmsMessageConverter(MessageConverter jmsMessageConverter) {
		Assert.notNull(jmsMessageConverter, "MessageConverter must not be null");
		this.jmsMessageConverter = jmsMessageConverter;
		this.converterSet = true;
	}

	/**
	 * Return the {@link MessageConverter} to use to convert a {@link Message}
	 * from the messaging to and from a {@link jakarta.jms.Message}.
	 */
	public MessageConverter getJmsMessageConverter() {
		return this.jmsMessageConverter;
	}

	/**
	 * Configure the default destination name to use in send methods that don't have
	 * a destination argument. If a default destination is not configured, send methods
	 * without a destination argument will raise an exception if invoked.
	 * @see #setDefaultDestination(Object)
	 */
	public void setDefaultDestinationName(@Nullable String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	/**
	 * Return the configured default destination name.
	 */
	@Nullable
	public String getDefaultDestinationName() {
		return this.defaultDestinationName;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.jmsTemplate, "Property 'connectionFactory' or 'jmsTemplate' is required");
		if (!this.converterSet && this.jmsTemplate.getMessageConverter() != null) {
			((MessagingMessageConverter) this.jmsMessageConverter)
					.setPayloadConverter(this.jmsTemplate.getMessageConverter());
		}
	}

	private JmsTemplate obtainJmsTemplate() {
		Assert.state(this.jmsTemplate != null, "No JmsTemplate set");
		return this.jmsTemplate;
	}


	@Override
	public void send(Message<?> message) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			send(defaultDestination, message);
		}
		else {
			send(getRequiredDefaultDestinationName(), message);
		}
	}

	@Override
	public void convertAndSend(Object payload) throws MessagingException {
		convertAndSend(payload, null);
	}

	@Override
	public void convertAndSend(Object payload, @Nullable MessagePostProcessor postProcessor) throws MessagingException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, payload, postProcessor);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), payload, postProcessor);
		}
	}

	@Override
	public void send(String destinationName, Message<?> message) throws MessagingException {
		doSend(destinationName, message);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload) throws MessagingException {
		convertAndSend(destinationName, payload, (Map<String, Object>) null);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, @Nullable Map<String, Object> headers)
			throws MessagingException {

		convertAndSend(destinationName, payload, headers, null);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(destinationName, payload, null, postProcessor);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		Message<?> message = doConvert(payload, headers, postProcessor);
		send(destinationName, message);
	}

	@Override
	@Nullable
	public Message<?> receive() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receive(defaultDestination);
		}
		else {
			return receive(getRequiredDefaultDestinationName());
		}
	}

	@Override
	@Nullable
	public <T> T receiveAndConvert(Class<T> targetClass) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receiveAndConvert(defaultDestination, targetClass);
		}
		else {
			return receiveAndConvert(getRequiredDefaultDestinationName(), targetClass);
		}
	}

	@Override
	@Nullable
	public Message<?> receive(String destinationName) throws MessagingException {
		return doReceive(destinationName);
	}

	@Override
	@Nullable
	public <T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException {
		Message<?> message = doReceive(destinationName);
		if (message != null) {
			return doConvert(message, targetClass);
		}
		else {
			return null;
		}
	}

	@Override
	@Nullable
	public Message<?> sendAndReceive(Message<?> requestMessage) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return sendAndReceive(defaultDestination, requestMessage);
		}
		else {
			return sendAndReceive(getRequiredDefaultDestinationName(), requestMessage);
		}
	}

	@Override
	@Nullable
	public Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException {
		return doSendAndReceive(destinationName, requestMessage);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException {

		return convertSendAndReceive(destinationName, request, null, targetClass);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass) {
		return convertSendAndReceive(request, targetClass, null);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass) throws MessagingException {

		return convertSendAndReceive(destinationName, request, headers, targetClass, null);
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(Object request, Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return convertSendAndReceive(defaultDestination, request, targetClass, postProcessor);
		}
		else {
			return convertSendAndReceive(getRequiredDefaultDestinationName(), request, targetClass, postProcessor);
		}
	}

	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor requestPostProcessor) throws MessagingException {

		return convertSendAndReceive(destinationName, request, null, targetClass, requestPostProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers,
			Class<T> targetClass, @Nullable MessagePostProcessor postProcessor) {

		Message<?> requestMessage = doConvert(request, headers, postProcessor);
		Message<?> replyMessage = sendAndReceive(destinationName, requestMessage);
		return (replyMessage != null ? (T) getMessageConverter().fromMessage(replyMessage, targetClass) : null);
	}

	@Override
	protected void doSend(Destination destination, Message<?> message) {
		try {
			obtainJmsTemplate().send(destination, createMessageCreator(message));
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	protected void doSend(String destinationName, Message<?> message) {
		try {
			obtainJmsTemplate().send(destinationName, createMessageCreator(message));
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Override
	@Nullable
	protected Message<?> doReceive(Destination destination) {
		try {
			jakarta.jms.Message jmsMessage = obtainJmsTemplate().receive(destination);
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Nullable
	protected Message<?> doReceive(String destinationName) {
		try {
			jakarta.jms.Message jmsMessage = obtainJmsTemplate().receive(destinationName);
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Override
	@Nullable
	protected Message<?> doSendAndReceive(Destination destination, Message<?> requestMessage) {
		try {
			jakarta.jms.Message jmsMessage = obtainJmsTemplate().sendAndReceive(
					destination, createMessageCreator(requestMessage));
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	@Nullable
	protected Message<?> doSendAndReceive(String destinationName, Message<?> requestMessage) {
		try {
			jakarta.jms.Message jmsMessage = obtainJmsTemplate().sendAndReceive(
					destinationName, createMessageCreator(requestMessage));
			return convertJmsMessage(jmsMessage);
		}
		catch (JmsException ex) {
			throw convertJmsException(ex);
		}
	}

	private MessagingMessageCreator createMessageCreator(Message<?> message) {
		return new MessagingMessageCreator(message, getJmsMessageConverter());
	}

	protected String getRequiredDefaultDestinationName() {
		String name = getDefaultDestinationName();
		if (name == null) {
			throw new IllegalStateException("No 'defaultDestination' or 'defaultDestinationName' specified. " +
					"Check configuration of JmsMessagingTemplate.");
		}
		return name;
	}

	@Nullable
	protected Message<?> convertJmsMessage(@Nullable jakarta.jms.Message message) {
		if (message == null) {
			return null;
		}
		try {
			return (Message<?>) getJmsMessageConverter().fromMessage(message);
		}
		catch (Exception ex) {
			throw new MessageConversionException("Could not convert '" + message + "'", ex);
		}
	}

	protected MessagingException convertJmsException(JmsException ex) {
		if (ex instanceof org.springframework.jms.support.destination.DestinationResolutionException ||
				ex instanceof InvalidDestinationException) {
			return new DestinationResolutionException(ex.getMessage(), ex);
		}
		if (ex instanceof org.springframework.jms.support.converter.MessageConversionException) {
			return new MessageConversionException(ex.getMessage(), ex);
		}
		// Fallback
		return new MessagingException(ex.getMessage(), ex);
	}


	private static class MessagingMessageCreator implements MessageCreator {

		private final Message<?> message;

		private final MessageConverter messageConverter;

		public MessagingMessageCreator(Message<?> message, MessageConverter messageConverter) {
			this.message = message;
			this.messageConverter = messageConverter;
		}

		@Override
		public jakarta.jms.Message createMessage(Session session) throws JMSException {
			try {
				return this.messageConverter.toMessage(this.message, session);
			}
			catch (Exception ex) {
				throw new MessageConversionException("Could not convert '" + this.message + "'", ex);
			}
		}
	}

}
