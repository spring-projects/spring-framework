/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.core;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

import org.springframework.jms.JmsException;

/**
 * Specifies a basic set of JMS operations.
 *
 * <p>Implemented by {@link JmsTemplate}. Not often used but a useful option
 * to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Provides <code>JmsTemplate's</code> <code>send(..)</code> and
 * <code>receive(..)</code> methods that mirror various JMS API methods.
 * See the JMS specification and javadocs for details on those methods.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see JmsTemplate
 * @see javax.jms.Destination
 * @see javax.jms.Session
 * @see javax.jms.MessageProducer
 * @see javax.jms.MessageConsumer
 */
public interface JmsOperations {

	/**
	 * Execute the action specified by the given action object within a JMS Session.
	 * <p>When used with a 1.0.2 provider, you may need to downcast
	 * to the appropriate domain implementation, either QueueSession or
	 * TopicSession in the action objects doInJms callback method.
	 * @param action callback object that exposes the session
	 * @return the result object from working with the session
	 * @throws JmsException if there is any problem
	 */
	<T> T execute(SessionCallback<T> action) throws JmsException;

	/**
	 * Send messages to the default JMS destination (or one specified
	 * for each send operation). The callback gives access to the JMS Session
	 * and MessageProducer in order to perform complex send operations.
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T execute(ProducerCallback<T> action) throws JmsException;

	/**
	 * Send messages to a JMS destination. The callback gives access to the JMS Session
	 * and MessageProducer in order to perform complex send operations.
	 * @param destination the destination to send messages to
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T execute(Destination destination, ProducerCallback<T> action) throws JmsException;

	/**
	 * Send messages to a JMS destination. The callback gives access to the JMS Session
	 * and MessageProducer in order to perform complex send operations.
	 * @param destinationName the name of the destination to send messages to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param action callback object that exposes the session/producer pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T execute(String destinationName, ProducerCallback<T> action) throws JmsException;


	//-------------------------------------------------------------------------
	// Convenience methods for sending messages
	//-------------------------------------------------------------------------

	/**
	 * Send a message to the default destination.
	 * <p>This will only work with a default destination specified!
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(MessageCreator messageCreator) throws JmsException;

	/**
	 * Send a message to the specified destination.
	 * The MessageCreator callback creates the message given a Session.
	 * @param destination the destination to send this message to
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * Send a message to the specified destination.
	 * The MessageCreator callback creates the message given a Session.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageCreator callback to create a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void send(String destinationName, MessageCreator messageCreator) throws JmsException;


	//-------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//-------------------------------------------------------------------------

	/**
	 * Send the given object to the default destination, converting the object
	 * to a JMS message with a configured MessageConverter.
	 * <p>This will only work with a default destination specified!
	 * @param message the object to convert to a message
	 * @throws JmsException converted checked JMSException to unchecked
	 */
	void convertAndSend(Object message) throws JmsException;

	/**
	 * Send the given object to the specified destination, converting the object
	 * to a JMS message with a configured MessageConverter.
	 * @param destination the destination to send this message to
	 * @param message the object to convert to a message
	 * @throws JmsException converted checked JMSException to unchecked
	 */
	void convertAndSend(Destination destination, Object message) throws JmsException;

	/**
	 * Send the given object to the specified destination, converting the object
	 * to a JMS message with a configured MessageConverter.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param message the object to convert to a message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(String destinationName, Object message) throws JmsException;

	/**
	 * Send the given object to the default destination, converting the object
	 * to a JMS message with a configured MessageConverter. The MessagePostProcessor
	 * callback allows for modification of the message after conversion.
	 * <p>This will only work with a default destination specified!
	 * @param message the object to convert to a message
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(Object message, MessagePostProcessor postProcessor)
	    throws JmsException;

	/**
	 * Send the given object to the specified destination, converting the object
	 * to a JMS message with a configured MessageConverter. The MessagePostProcessor
	 * callback allows for modification of the message after conversion.
	 * @param destination the destination to send this message to
	 * @param message the object to convert to a message
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor)
	    throws JmsException;

	/**
	 * Send the given object to the specified destination, converting the object
	 * to a JMS message with a configured MessageConverter. The MessagePostProcessor
	 * callback allows for modification of the message after conversion.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param message the object to convert to a message.
	 * @param postProcessor the callback to modify the message
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	void convertAndSend(String destinationName, Object message, MessagePostProcessor postProcessor)
	    throws JmsException;


	//-------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//-------------------------------------------------------------------------

	/**
	 * Receive a message synchronously from the default destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * <p>This will only work with a default destination specified!
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receive() throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destination the destination to receive a message from
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receive(Destination destination) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receive(String destinationName) throws JmsException;

	/**
	 * Receive a message synchronously from the default destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * <p>This will only work with a default destination specified!
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receiveSelected(String messageSelector) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destination the destination to receive a message from
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receiveSelected(Destination destination, String messageSelector) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message received by the consumer, or <code>null</code> if the timeout expires
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Message receiveSelected(String destinationName, String messageSelector) throws JmsException;


	//-------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//-------------------------------------------------------------------------

	/**
	 * Receive a message synchronously from the default destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * <p>This will only work with a default destination specified!
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveAndConvert() throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destination the destination to receive a message from
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveAndConvert(Destination destination) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveAndConvert(String destinationName) throws JmsException;

	/**
	 * Receive a message synchronously from the default destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * <p>This will only work with a default destination specified!
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveSelectedAndConvert(String messageSelector) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destination the destination to receive a message from
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException;

	/**
	 * Receive a message synchronously from the specified destination, but only
	 * wait up to a specified time for delivery. Convert the message into an
	 * object with a configured MessageConverter.
	 * <p>This method should be used carefully, since it will block the thread
	 * until the message becomes available or until the timeout value is exceeded.
	 * @param destinationName the name of the destination to send this message to
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @return the message produced for the consumer or <code>null</code> if the timeout expires.
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException;


	//-------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//-------------------------------------------------------------------------

	/**
	 * Browse messages in the default JMS queue. The callback gives access to the JMS
	 * Session and QueueBrowser in order to browse the queue and react to the contents.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browse(BrowserCallback<T> action) throws JmsException;

	/**
	 * Browse messages in a JMS queue. The callback gives access to the JMS Session
	 * and QueueBrowser in order to browse the queue and react to the contents.
	 * @param queue the queue to browse
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException;

	/**
	 * Browse messages in a JMS queue. The callback gives access to the JMS Session
	 * and QueueBrowser in order to browse the queue and react to the contents.
	 * @param queueName the name of the queue to browse
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browse(String queueName, BrowserCallback<T> action) throws JmsException;

	/**
	 * Browse selected messages in a JMS queue. The callback gives access to the JMS
	 * Session and QueueBrowser in order to browse the queue and react to the contents.
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * Browse selected messages in a JMS queue. The callback gives access to the JMS
	 * Session and QueueBrowser in order to browse the queue and react to the contents.
	 * @param queue the queue to browse
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browseSelected(Queue queue, String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * Browse selected messages in a JMS queue. The callback gives access to the JMS
	 * Session and QueueBrowser in order to browse the queue and react to the contents.
	 * @param queueName the name of the queue to browse
	 * (to be resolved to an actual destination by a DestinationResolver)
	 * @param messageSelector the JMS message selector expression (or <code>null</code> if none).
	 * See the JMS specification for a detailed definition of selector expressions.
	 * @param action callback object that exposes the session/browser pair
	 * @return the result object from working with the session
	 * @throws JmsException checked JMSException converted to unchecked
	 */
	<T> T browseSelected(String queueName, String messageSelector, BrowserCallback<T> action) throws JmsException;

}
