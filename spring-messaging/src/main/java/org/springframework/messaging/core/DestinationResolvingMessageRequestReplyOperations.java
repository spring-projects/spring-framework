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
 * A {@link MessageRequestReplyOperations} that can resolve a String-based destinations.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface DestinationResolvingMessageRequestReplyOperations<D> extends MessageRequestReplyOperations<D> {

	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException;

	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass) throws MessagingException;

	<T> T convertSendAndReceive(String destinationName, Object request,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
