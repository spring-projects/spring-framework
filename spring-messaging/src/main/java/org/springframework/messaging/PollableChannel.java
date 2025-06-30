/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.messaging;

import org.jspecify.annotations.Nullable;

/**
 * A {@link MessageChannel} from which messages may be actively received through polling.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public interface PollableChannel extends MessageChannel {

	/**
	 * Receive a message from this channel, blocking indefinitely if necessary.
	 * @return the next available {@link Message} or {@code null} if interrupted
	 */
	@Nullable Message<?> receive();

	/**
	 * Receive a message from this channel, blocking until either a message is available
	 * or the specified timeout period elapses.
	 * @param timeout the timeout in milliseconds or {@link MessageChannel#INDEFINITE_TIMEOUT}
	 * @return the next available {@link Message} or {@code null} if the specified timeout
	 * period elapses or the message receipt is interrupted
	 */
	@Nullable Message<?> receive(long timeout);

}
