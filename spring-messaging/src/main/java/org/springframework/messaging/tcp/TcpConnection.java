/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * A contract for sending messages and managing a TCP connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> the type of payload for outbound {@link Message Messages}
 */
public interface TcpConnection<P> extends Closeable {

	/**
	 * Send the given message.
	 * @param message the message
	 * @return a CompletableFuture that can be used to determine when and if the
	 * message was successfully sent
	 * @since 6.0
	 */
	CompletableFuture<@Nullable Void> sendAsync(Message<P> message);

	/**
	 * Register a task to invoke after a period of read inactivity.
	 * @param runnable the task to invoke
	 * @param duration the amount of inactive time in milliseconds
	 */
	void onReadInactivity(Runnable runnable, long duration);

	/**
	 * Register a task to invoke after a period of write inactivity.
	 * @param runnable the task to invoke
	 * @param duration the amount of inactive time in milliseconds
	 */
	void onWriteInactivity(Runnable runnable, long duration);

	/**
	 * Close the connection.
	 */
	@Override
	void close();

}
