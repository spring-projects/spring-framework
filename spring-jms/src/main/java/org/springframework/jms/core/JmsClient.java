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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * A fluent {@code JmsClient} with common send and receive operations against a JMS
 * destination. This is effectively an alternative to {@link JmsMessagingTemplate},
 * also delegating to Spring's {@link JmsTemplate} for performing actual operations.
 *
 * <p>Note: Operations in this interface throw {@link MessagingException} instead of
 * the JMS-specific {@link org.springframework.jms.JmsException}, aligning with the
 * {@code spring-messaging} module and its other client operation handles.
 *
 * <p>This client provides reusable operation handles which can be configured with
 * custom QoS settings. Note that any use of such explicit settings will override
 * administrative provider settings (see {@link JmsTemplate#setExplicitQosEnabled}).
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see JmsTemplate
 * @see JmsMessagingTemplate
 */
interface JmsClient {

	/**
	 * Provide an operation handle for the given JMS destination.
	 * @param destination the JMS {@code Destination} object
	 * @return a reusable operation handle bound to the destination
	 */
	OperationSpec destination(Destination destination);

	/**
	 * Provide an operation handle for the specified JMS destination.
	 * @param destinationName a name resolving to a JMS {@code Destination}
	 * @return a reusable operation handle bound to the destination
	 * @see org.springframework.jms.support.destination.DestinationResolver
	 */
	OperationSpec destination(String destinationName);


	// Static factory methods

	/**
	 * Create a new {@code JmsClient} for the given {@link ConnectionFactory}.
	 * @param connectionFactory the factory to obtain JMS connections from
	 */
	static JmsClient create(ConnectionFactory connectionFactory) {
		return new DefaultJmsClient(connectionFactory, null);
	}

	/**
	 * Create a new {@code JmsClient} for the given {@link ConnectionFactory}.
	 * @param connectionFactory the factory to obtain JMS connections from
	 * @param messageConverter the message converter for payload objects
	 */
	static JmsClient create(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		return new DefaultJmsClient(connectionFactory, messageConverter);
	}

	/**
	 * Create a new {@code JmsClient} for the given {@link JmsOperations}.
	 * @param jmsTemplate the {@link JmsTemplate} to use for performing operations
	 * (can be a custom {@link JmsOperations} implementation as well)
	 */
	static JmsClient create(JmsOperations jmsTemplate) {
		return new DefaultJmsClient(jmsTemplate, null);
	}

	/**
	 * Create a new {@code JmsClient} for the given {@link JmsOperations}.
	 * @param jmsTemplate the {@link JmsTemplate} to use for performing operations
	 * (can be a custom {@link JmsOperations} implementation as well)
	 * @param messageConverter the message converter for payload objects
	 */
	static JmsClient create(JmsOperations jmsTemplate, MessageConverter messageConverter) {
		return new DefaultJmsClient(jmsTemplate, messageConverter);
	}


	/**
	 * Common JMS send and receive operations with various settings.
	 */
	interface OperationSpec {

		/**
		 * Apply the given timeout to any subsequent receive operations.
		 * @param receiveTimeout the timeout in milliseconds
		 * @see JmsTemplate#setReceiveTimeout
		 */
		OperationSpec withReceiveTimeout(long receiveTimeout);

		/**
		 * Apply the given delivery delay to any subsequent send operations.
		 * @param deliveryDelay the delay in milliseconds
		 * @see JmsTemplate#setDeliveryDelay
		 */
		OperationSpec withDeliveryDelay(long deliveryDelay);

		/**
		 * Set whether message delivery should be persistent or non-persistent.
		 * @param persistent to choose between delivery mode "PERSISTENT"
		 * ({@code true}) or "NON_PERSISTENT" ({@code false})
		 * @see JmsTemplate#setDeliveryPersistent
		 */
		OperationSpec withDeliveryPersistent(boolean persistent);

		/**
		 * Apply the given priority to any subsequent send operations.
		 * @param priority the priority value
		 * @see JmsTemplate#setPriority
		 */
		OperationSpec withPriority(int priority);

		/**
		 * Apply the given time-to-live to any subsequent send operations.
		 * @param timeToLive the message lifetime in milliseconds
		 * @see JmsTemplate#setTimeToLive
		 */
		OperationSpec withTimeToLive(long timeToLive);

		/**
		 * Send the given {@link Message} to the pre-bound destination.
		 * @param message the spring-messaging {@link Message} to send
		 * @see #withDeliveryDelay
		 * @see #withDeliveryPersistent
		 * @see #withPriority
		 * @see #withTimeToLive
		 */
		void send(Message<?> message) throws MessagingException;

		/**
		 * Send a message with the given payload to the pre-bound destination.
		 * @param payload the payload to convert into a {@link Message}
		 * @see #withDeliveryDelay
		 * @see #withDeliveryPersistent
		 * @see #withPriority
		 * @see #withTimeToLive
		 */
		void send(Object payload) throws MessagingException;

		/**
		 * Send a message with the given payload to the pre-bound destination.
		 * @param payload the payload to convert into a {@link Message}
		 * @param headers the message headers to apply to the {@link Message}
		 * @see #withDeliveryDelay
		 * @see #withDeliveryPersistent
		 * @see #withPriority
		 * @see #withTimeToLive
		 */
		void send(Object payload, Map<String, Object> headers) throws MessagingException;

		/**
		 * Receive a {@link Message} from the pre-bound destination.
		 * @return the spring-messaging {@link Message} received,
		 * or {@link Optional#empty()} if none
		 * @see #withReceiveTimeout
		 */
		Optional<Message<?>> receive() throws MessagingException;

		/**
		 * Receive a {@link Message} from the pre-bound destination,
		 * extracting and converting its payload.
		 * @param targetClass the class to convert the payload to
		 * @return the payload of the {@link Message} received,
		 * or {@link Optional#empty()} if none
		 * @see #withReceiveTimeout
		 */
		<T> Optional<T> receive(Class<T> targetClass) throws MessagingException;

		/**
		 * Receive a {@link Message} from the pre-bound destination.
		 * @param messageSelector the JMS message selector to apply
		 * @return the spring-messaging {@link Message} received,
		 * or {@link Optional#empty()} if none
		 * @see #withReceiveTimeout
		 */
		Optional<Message<?>> receive(String messageSelector) throws MessagingException;

		/**
		 * Receive a {@link Message} from the pre-bound destination,
		 * extracting and converting its payload.
		 * @param targetClass the class to convert the payload to
		 * @return the payload of the {@link Message} received,
		 * or {@link Optional#empty()} if none
		 * @param messageSelector the JMS message selector to apply
		 * @see #withReceiveTimeout
		 */
		<T> Optional<T> receive(String messageSelector, Class<T> targetClass) throws MessagingException;

		/**
		 * Send a request message and receive the reply from the given destination.
		 * @param requestMessage the spring-messaging {@link Message} to send
		 * @return the spring-messaging {@link Message} received as a reply,
		 * or {@link Optional#empty()} if none
		 * @see #withReceiveTimeout
		 */
		Optional<Message<?>> sendAndReceive(Message<?> requestMessage) throws MessagingException;

		/**
		 * Send a request message and receive the reply from the given destination.
		 * @param request the payload to convert into a request {@link Message}
		 * @param targetClass the class to convert the reply's payload to
		 * @return the payload of the {@link Message} received as a reply,
		 * or {@link Optional#empty()} if none
		 * @see #withTimeToLive
		 * @see #withReceiveTimeout
		 */
		<T> Optional<T> sendAndReceive(Object request, Class<T> targetClass) throws MessagingException;

		/**
		 * Send a request message and receive the reply from the given destination.
		 * @param request the payload to convert into a request {@link Message}
		 * @param headers the message headers to apply to the request {@link Message}
		 * @param targetClass the class to convert the reply's payload to
		 * @return the payload of the {@link Message} received as a reply,
		 * or {@link Optional#empty()} if none
		 * @see #withTimeToLive
		 * @see #withReceiveTimeout
		 */
		<T> Optional<T> sendAndReceive(Object request, Map<String, Object> headers, Class<T> targetClass)
				throws MessagingException;
	}

}
