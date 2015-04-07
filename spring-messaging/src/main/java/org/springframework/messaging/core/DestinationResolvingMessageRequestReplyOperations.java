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

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Extends {@link MessageRequestReplyOperations} and adds operations for sending and
 * receiving messages to and from a destination specified as a (resolvable) String name.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see DestinationResolver
 */
public interface DestinationResolvingMessageRequestReplyOperations<D> extends MessageRequestReplyOperations<D> {

	/**
	 * Resolve the given destination name to a destination and send the given message,
	 * receive a reply and return it.
	 * @param destinationName the name of the target destination
	 * @param requestMessage the mesage to send
	 * @return the received message, possibly {@code null} if the message could not
	 * be received, for example due to a timeout
	 */
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * Resolve the given destination name, convert the payload request Object
	 * to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message and send it to the resolved destination, receive a reply
	 * and convert its body to the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException;

	/**
	 * Resolve the given destination name, convert the payload request Object
	 * to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers and send it to the resolved destination,
	 * receive a reply and convert its body to the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass) throws MessagingException;

	/**
	 * Resolve the given destination name, convert the payload request Object
	 * to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message, apply the given post process, and send the resulting
	 * message to the resolved destination, then receive a reply and convert its
	 * body to the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @param requestPostProcessor post process for the request message
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> T convertSendAndReceive(String destinationName, Object request,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * Resolve the given destination name, convert the payload request Object
	 * to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post process,
	 * and send the resulting message to the resolved destination, then receive
	 * a reply and convert its body to the specified target class.
	 * @param destinationName the name of the target destination
	 * @param request the payload for the request message to send
	 * @param headers the headers for the request message to send
	 * @param targetClass the target class to convert the payload of the reply to
	 * @param requestPostProcessor post process for the request message
	 * @return the converted payload of the reply message, possibly {@code null} if
	 * the message could not be received, for example due to a timeout
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
