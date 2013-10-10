/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;


/**
 * A specialization of {@link MessageSendingOperations} with methods for use with
 * the Spring Framework support for simple messaging protocols (like STOMP).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SimpMessageSendingOperations extends MessageSendingOperations<String> {

	/**
	 * Send a message to a specific user.
	 *
	 * @param user the user that should receive the message.
	 * @param destination the destination to send the message to.
	 * @param payload the payload to send
	 */
	void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException;

	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * Send a message to a specific user.
	 *
	 * @param user the user that should receive the message.
	 * @param destination the destination to send the message to.
	 * @param payload the payload to send
	 * @param postProcessor a postProcessor to post-process or modify the created message
	 */
	void convertAndSendToUser(String user, String destination, Object payload,
			MessagePostProcessor postProcessor) throws MessagingException;

	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException;

}
