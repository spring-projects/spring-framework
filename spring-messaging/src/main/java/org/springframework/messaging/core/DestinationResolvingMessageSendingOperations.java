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

package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Extends {@link MessageSendingOperations} and adds operations for sending messages
 * to a destination specified as a (resolvable) String name.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageSendingOperations<D> extends MessageSendingOperations<D> {

	/**
	 * Resolve the given destination name to a destination and send a message to it.
	 * @param destinationName the destination name to resolve
	 * @param message the message to send
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * Resolve the given destination name to a destination, convert the payload Object
	 * to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message and send it to the resolved destination.
	 * @param destinationName the destination name to resolve
   	 * @param payload the Object to use as payload
	 */
	<T> void convertAndSend(String destinationName, T payload) throws MessagingException;

	/**
	 * Resolve the given destination name to a destination, convert the payload
	 * Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers and send it to the resolved
	 * destination.
	 * @param destinationName the destination name to resolve
	 * @param payload the Object to use as payload
 	 * @param headers headers for the message to send
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers)
			throws MessagingException;

	/**
	 * Resolve the given destination name to a destination, convert the payload
	 * Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message, apply the given post processor, and send the resulting
	 * message to the resolved destination.
	 * @param destinationName the destination name to resolve
	 * @param payload the Object to use as payload
	 * @param postProcessor the post processor to apply to the message
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * Resolve the given destination name to a destination, convert the payload
	 * Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post processor,
	 * and send the resulting message to the resolved destination.
	 * @param destinationName the destination name to resolve
	 * @param payload the Object to use as payload
	 * @param headers headers for the message to send
	 * @param postProcessor the post processor to apply to the message
	 */
	<T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

}
