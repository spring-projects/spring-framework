/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket;

/**
 * A message that can be handled or sent on a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketMessage<T> {

	/**
	 * Return the message payload (never {@code null}).
	 */
	T getPayload();

	/**
	 * Return the number of bytes contained in the message.
	 */
	int getPayloadLength();

	/**
	 * When partial message support is available and requested via
	 * {@link org.springframework.web.socket.WebSocketHandler#supportsPartialMessages()},
	 * this method returns {@code true} if the current message is the last part of the
	 * complete WebSocket message sent by the client. Otherwise {@code false} is returned
	 * if partial message support is either not available or not enabled.
	 */
	boolean isLast();

}
