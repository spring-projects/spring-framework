/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link StompSession} that implements
 * {@link org.springframework.messaging.tcp.TcpConnectionHandler
 * TcpConnectionHandler} in order to send and receive messages.
 *
 * <p>A ConnectionHandlingStompSession can be used with any TCP or WebSocket
 * library that is adapted to the {@code TcpConnectionHandler} contract.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface ConnectionHandlingStompSession extends StompSession, StompTcpConnectionHandler<byte[]> {

	/**
	 * Return a future that will complete when the session is ready for use.
	 * @since 6.0
	 */
	CompletableFuture<StompSession> getSession();

}
