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

package org.springframework.messaging;

/**
 * Base channel interface defining common behavior for sending messages.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public interface MessageChannel {

	/**
	 * Constant for sending a message without a prescribed timeout.
	 */
	public static final long INDEFINITE_TIMEOUT = -1;


	/**
	 * Send a {@link Message} to this channel. May throw a RuntimeException for
	 * non-recoverable errors. Otherwise, if the Message cannot be sent for a non-fatal
	 * reason this method will return 'false', and if the Message is sent successfully, it
	 * will return 'true'.
	 *
	 * <p>Depending on the implementation, this method may block indefinitely. To provide a
	 * maximum wait time, use {@link #send(Message, long)}.
	 * @param message the {@link Message} to send
	 * @return whether or not the Message has been sent successfully
	 */
	boolean send(Message<?> message);

	/**
	 * Send a message, blocking until either the message is accepted or the specified
	 * timeout period elapses.
	 * @param message the {@link Message} to send
	 * @param timeout the timeout in milliseconds or #INDEFINITE_TIMEOUT
	 * @return {@code true} if the message is sent successfully, {@code false} if the
	 *         specified timeout period elapses or the send is interrupted
	 */
	boolean send(Message<?> message, long timeout);

}
