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

package org.springframework.messaging.support.tcp;

import org.springframework.messaging.Message;

/**
 * A contract for managing lifecycle events for a TCP connection including
 * the handling of incoming messages.
 *
 * @param <P> the type of payload for in and outbound messages
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface TcpConnectionHandler<P> {

	/**
	 * Invoked after a connection is successfully established.
	 * @param connection the connection
	 */
	void afterConnected(TcpConnection<P> connection);

	/**
	 * Invoked after a connection failure.
	 * @param ex the exception
	 */
	void afterConnectFailure(Throwable ex);

	/**
	 * Handle a message received from the remote host.
	 * @param message the message
	 */
	void handleMessage(Message<P> message);

	/**
	 * Invoked after the connection is closed.
	 */
	void afterConnectionClosed();

}
