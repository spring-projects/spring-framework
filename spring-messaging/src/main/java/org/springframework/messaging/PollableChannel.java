/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
