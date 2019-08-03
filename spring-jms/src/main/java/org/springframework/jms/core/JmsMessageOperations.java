/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.jms.Destination;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageReceivingOperations;
import org.springframework.messaging.core.MessageRequestReplyOperations;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * A specialization of {@link MessageSendingOperations}, {@link MessageReceivingOperations}
 * and {@link MessageRequestReplyOperations} for JMS related operations that allow to specify
 * a destination name rather than the actual {@link javax.jms.Destination}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.jms.core.JmsTemplate
 * @see org.springframework.messaging.core.MessageSendingOperations
 * @see org.springframework.messaging.core.MessageReceivingOperations
 * @see org.springframework.messaging.core.MessageRequestReplyOperations
 */
public interface JmsMessageOperations extends MessageSendingOperations<Destination>,
		MessageReceivingOperations<Destination>, MessageRequestReplyOperations<Destination> {

	/**
	 * Send a message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param message the message to send
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message and send it to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 */
	void convertAndSend(String destinationName, Object payload) throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers and send it to
	 * the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers headers for the message to send
	 */
	void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message, apply the given post processor, and send
	 * the resulting message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * Convert the given Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post processor,
	 * and send the resulting message to the given destination.
	 * @param destinationName the name of the target destination
	 * @param payload the Object to use as payload
	 * @param headers headers for the message to send
	 * @param postProcessor the post processor to apply to the message
	 */
	void convertAndSend(String destinationName, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * Receive a message from the given destination.
	 * @param destinationName the name of the target destination
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	@Nullable
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * Receive a message from the given destination and convert its payload to the
	 * specified target class.
	 * @param destinationName the name of the target destination
	 * @param targetClass the target class to convert the payload to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

	/**
	 * Send a request message and receive the reply from the given destination.
	 * @param destinationName the name of the target destination
	 * @param requestMessage the message to send
	 * @return the reply, possibly {@code null} if the message could not be received,
	 * for example due to a timeout
	 */
	@Nullable
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter}, send
	 * it as a {@link Message} to the given destination, receive the reply and convert
	 * its body of the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter}, send
	 * it as a {@link Message} with the given headers, to the specified destination,
	 * receive the reply and convert its body of the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param headers headers for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, @Nullable Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * apply the given post processor and send the resulting {@link Message} to the
	 * given destination, receive the reply and convert its body of the given
	 * target class.
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post processor
	 * and send the resulting {@link Message} to the specified destination, receive
	 * the reply and convert its body of the given target class.
	 * @param destinationName the name of the target destination
	 * @param request payload for the request message to send
	 * @param targetClass the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return the payload of the reply message, possibly {@code null} if the message
	 * could not be received, for example due to a timeout
	 */
	@Nullable
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
