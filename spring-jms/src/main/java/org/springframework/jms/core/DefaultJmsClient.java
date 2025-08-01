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

package org.springframework.jms.core;

import java.util.Map;
import java.util.Optional;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import org.jspecify.annotations.Nullable;

import org.springframework.jms.support.JmsAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * The default implementation of {@link JmsClient},
 * as created by the static factory methods.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 7.0
 * @see JmsClient#create(ConnectionFactory)
 * @see JmsClient#create(JmsOperations)
 */
class DefaultJmsClient implements JmsClient {

	private final JmsOperations jmsTemplate;

	private @Nullable MessageConverter messageConverter;

	private @Nullable MessagePostProcessor messagePostProcessor;


	public DefaultJmsClient(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.jmsTemplate = new JmsTemplate(connectionFactory);
	}

	public DefaultJmsClient(JmsOperations jmsTemplate) {
		Assert.notNull(jmsTemplate, "JmsTemplate must not be null");
		this.jmsTemplate = jmsTemplate;
	}

	void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	void setMessagePostProcessor(MessagePostProcessor messagePostProcessor) {
		Assert.notNull(messagePostProcessor, "MessagePostProcessor must not be null");
		this.messagePostProcessor = messagePostProcessor;
	}


	public OperationSpec destination(Destination destination) {
		return new DefaultOperationSpec(destination);
	}

	public OperationSpec destination(String destinationName) {
		return new DefaultOperationSpec(destinationName);
	}

	private JmsMessagingTemplate newDelegate() {
		JmsMessagingTemplate delegate = new JmsMessagingTemplate(DefaultJmsClient.this.jmsTemplate);
		MessageConverter converter = DefaultJmsClient.this.messageConverter;
		if (converter != null) {
			delegate.setMessageConverter(converter);
		}
		return delegate;
	}


	private class DefaultOperationSpec implements OperationSpec {

		private final JmsMessagingTemplate delegate;

		private @Nullable JmsTemplate customTemplate;

		public DefaultOperationSpec(Destination destination) {
			this.delegate = newDelegate();
			this.delegate.setDefaultDestination(destination);
		}

		public DefaultOperationSpec(String destinationName) {
			this.delegate = newDelegate();
			this.delegate.setDefaultDestinationName(destinationName);
		}

		private JmsTemplate enforceCustomTemplate(boolean qos) {
			if (this.customTemplate == null) {
				JmsOperations jmsOperations = DefaultJmsClient.this.jmsTemplate;
				if (!(jmsOperations instanceof JmsAccessor original)) {
					throw new IllegalStateException(
							"Needs to be bound to a JmsAccessor for custom settings support: " + jmsOperations);
				}
				this.customTemplate = new JmsTemplate(original);
				this.delegate.setJmsTemplate(this.customTemplate);
			}
			if (qos) {
				this.customTemplate.setExplicitQosEnabled(true);
			}
			return this.customTemplate;
		}

		@Override
		public OperationSpec withReceiveTimeout(long receiveTimeout) {
			enforceCustomTemplate(false).setReceiveTimeout(receiveTimeout);
			return this;
		}

		@Override
		public OperationSpec withDeliveryDelay(long deliveryDelay) {
			enforceCustomTemplate(false).setDeliveryDelay(deliveryDelay);
			return this;
		}

		@Override
		public OperationSpec withDeliveryPersistent(boolean persistent) {
			enforceCustomTemplate(true).setDeliveryPersistent(persistent);
			return this;
		}

		@Override
		public OperationSpec withPriority(int priority) {
			enforceCustomTemplate(true).setPriority(priority);
			return this;
		}

		@Override
		public OperationSpec withTimeToLive(long timeToLive) {
			enforceCustomTemplate(true).setTimeToLive(timeToLive);
			return this;
		}

		@Override
		public void send(Message<?> message) throws MessagingException {
			message = postProcessMessage(message);
			this.delegate.send(message);
		}

		@Override
		public void send(Object payload) throws MessagingException {
			this.delegate.convertAndSend(payload, DefaultJmsClient.this.messagePostProcessor);
		}

		@Override
		public void send(Object payload, Map<String, Object> headers) throws MessagingException {
			this.delegate.convertAndSend(payload, headers, DefaultJmsClient.this.messagePostProcessor);
		}

		@Override
		public Optional<Message<?>> receive() throws MessagingException {
			return Optional.ofNullable(this.delegate.receive());
		}

		@Override
		public <T> Optional<T> receive(Class<T> targetClass) throws MessagingException {
			return Optional.ofNullable(this.delegate.receiveAndConvert(targetClass));
		}

		@Override
		public Optional<Message<?>> receive(String messageSelector) throws MessagingException {
			return Optional.ofNullable(this.delegate.receiveSelected(messageSelector));
		}

		@Override
		public <T> Optional<T> receive(String messageSelector, Class<T> targetClass) throws MessagingException {
			return Optional.ofNullable(this.delegate.receiveSelectedAndConvert(messageSelector, targetClass));
		}

		@Override
		public Optional<Message<?>> sendAndReceive(Message<?> requestMessage) throws MessagingException {
			requestMessage = postProcessMessage(requestMessage);
			return Optional.ofNullable(this.delegate.sendAndReceive(requestMessage));
		}

		@Override
		public <T> Optional<T> sendAndReceive(Object request, Class<T> targetClass) throws MessagingException {
			return Optional.ofNullable(this.delegate.convertSendAndReceive(request, targetClass, DefaultJmsClient.this.messagePostProcessor));
		}

		@Override
		public <T> Optional<T> sendAndReceive(Object request, Map<String, Object> headers, Class<T> targetClass)
				throws MessagingException {

			return Optional.ofNullable(this.delegate.convertSendAndReceive(request, headers, targetClass, DefaultJmsClient.this.messagePostProcessor));
		}

		private Message<?> postProcessMessage(Message<?> message) {
			if (DefaultJmsClient.this.messagePostProcessor != null) {
				return DefaultJmsClient.this.messagePostProcessor.postProcessMessage(message);
			}
			return message;
		}
	}

}
