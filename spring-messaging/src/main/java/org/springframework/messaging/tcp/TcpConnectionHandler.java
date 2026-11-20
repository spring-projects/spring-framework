/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp;

import org.springframework.messaging.Message;

/**
 * A contract for managing lifecycle events for a TCP connection including
 * the handling of incoming messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> the type of payload for in and outbound messages
 */
public interface TcpConnectionHandler<P> {

	/**
	 * Invoked after a connection is successfully established.
	 * @param connection the connection
	 */
	void afterConnected(TcpConnection<P> connection);

	/**
	 * Invoked on failure to connect.
	 * @param ex the exception
	 */
	void afterConnectFailure(Throwable ex);

	/**
	 * Handle a message received from the remote host.
	 * @param message the message
	 */
	void handleMessage(Message<P> message);

	/**
	 * Handle a failure on the connection.
	 * @param ex the exception
	 */
	void handleFailure(Throwable ex);

	/**
	 * Invoked after the connection is closed.
	 */
	void afterConnectionClosed();

}
