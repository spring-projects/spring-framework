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
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A contract for sending messages and managing a TCP connection.
 *
 * @param <P> the type of payload for outbound {@link Message}s
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface TcpConnection<P> {

	/**
	 * Send the given message.
	 * @param message the message
	 * @return whether the send succeeded or not
	 */
	ListenableFuture<Void> send(Message<P> message);

	/**
	 * Register a task to invoke after a period of of read inactivity.
	 * @param runnable the task to invoke
	 * @param duration the amount of inactive time in milliseconds
	 */
	void onReadInactivity(Runnable runnable, long duration);

	/**
	 * Register a task to invoke after a period of of write inactivity.
	 * @param runnable the task to invoke
	 * @param duration the amount of inactive time in milliseconds
	 */
	void onWriteInactivity(Runnable runnable, long duration);

	/**
	 * Close the connection.
	 */
	void close();

}
