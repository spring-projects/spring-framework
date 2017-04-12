/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * A specialization of {@link MessageSendingOperations} with methods for use with
 * the Spring Framework support for Simple Messaging Protocols (like STOMP).
 *
 * <p>For more on user destinations see
 * {@link org.springframework.messaging.simp.user.UserDestinationResolver
 * UserDestinationResolver}.
 *
 * <p>Generally it is expected the user is the one authenticated with the
 * WebSocket session (or by extension the user authenticated with the
 * handshake request that started the session). However if the session is
 * not authenticated, it is also possible to pass the session id (if known)
 * in place of the user name. Keep in mind though that in that scenario,
 * you must use one of the overloaded methods that accept headers making sure the
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor#setSessionId
 * sessionId} header has been set accordingly.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SimpMessageSendingOperations extends MessageSendingOperations<String> {

	/**
	 * Send a message to the given user.
	 * @param user the user that should receive the message.
	 * @param destination the destination to send the message to.
	 * @param payload the payload to send
	 */
	void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException;

	/**
	 * Send a message to the given user.
	 * <p>By default headers are interpreted as native headers (e.g. STOMP) and
	 * are saved under a special key in the resulting Spring
	 * {@link org.springframework.messaging.Message Message}. In effect when the
	 * message leaves the application, the provided headers are included with it
	 * and delivered to the destination (e.g. the STOMP client or broker).
	 * <p>If the map already contains the key
	 * {@link org.springframework.messaging.support.NativeMessageHeaderAccessor#NATIVE_HEADERS "nativeHeaders"}
	 * or was prepared with
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor SimpMessageHeaderAccessor}
	 * then the headers are used directly. A common expected case is providing a
	 * content type (to influence the message conversion) and native headers.
	 * This may be done as follows:
	 * <pre class="code">
	 * SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
	 * accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);
	 * accessor.setNativeHeader("foo", "bar");
	 * accessor.setLeaveMutable(true);
	 * MessageHeaders headers = accessor.getMessageHeaders();
	 * messagingTemplate.convertAndSendToUser(user, destination, payload, headers);
	 * </pre>
	 * <p><strong>Note:</strong> if the {@code MessageHeaders} are mutable as in
	 * the above example, implementations of this interface should take notice and
	 * update the headers in the same instance (rather than copy or re-create it)
	 * and then set it immutable before sending the final message.
	 * @param user the user that should receive the message (must not be {@code null})
	 * @param destination the destination to send the message to (must not be {@code null})
	 * @param payload the payload to send (may be {@code null})
	 * @param headers the message headers (may be {@code null})
	 */
	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * Send a message to the given user.
	 * @param user the user that should receive the message (must not be {@code null})
	 * @param destination the destination to send the message to (must not be {@code null})
	 * @param payload the payload to send (may be {@code null})
	 * @param postProcessor a postProcessor to post-process or modify the created message
	 */
	void convertAndSendToUser(String user, String destination, Object payload,
			MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * Send a message to the given user.
	 * <p>See {@link #convertAndSend(Object, Object, java.util.Map)} for important
	 * notes regarding the input headers.
	 * @param user the user that should receive the message
	 * @param destination the destination to send the message to
	 * @param payload the payload to send
	 * @param headers the message headers
	 * @param postProcessor a postProcessor to post-process or modify the created message
	 */
	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException;

}
