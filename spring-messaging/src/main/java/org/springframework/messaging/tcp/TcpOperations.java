/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;


/**
 * A contract for establishing TCP connections.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> the type of payload for inbound and outbound messages
 */
public interface TcpOperations<P> {

	/**
	 * Open a new connection.
	 * @param connectionHandler a handler to manage the connection
	 * @return a CompletableFuture that can be used to determine when and if the
	 * connection is successfully established
	 * @since 6.0
	 */
	CompletableFuture<@Nullable Void> connectAsync(TcpConnectionHandler<P> connectionHandler);

	/**
	 * Open a new connection and a strategy for reconnecting if the connection fails.
	 * @param connectionHandler a handler to manage the connection
	 * @param reconnectStrategy a strategy for reconnecting
	 * @return a CompletableFuture that can be used to determine when and if the
	 * initial connection is successfully established
	 * @since 6.0
	 */
	CompletableFuture<@Nullable Void> connectAsync(TcpConnectionHandler<P> connectionHandler, ReconnectStrategy reconnectStrategy);

	/**
	 * Shut down and close any open connections.
	 * @return a CompletableFuture that can be used to determine when and if the
	 * connection is successfully closed
	 * @since 6.0
	 */
	CompletableFuture<@Nullable Void> shutdownAsync();

}
