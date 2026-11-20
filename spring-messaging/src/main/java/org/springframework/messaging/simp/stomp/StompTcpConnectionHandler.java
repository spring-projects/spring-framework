/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnectionHandler;

/**
 * A {@link TcpConnectionHandler} for use with STOMP connections, exposing
 * further information about the connection.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 * @param <P> the type of payload for inbound and outbound messages
 */
public interface StompTcpConnectionHandler<P> extends TcpConnectionHandler<P> {

	/**
	 * Return the {@link SimpMessageHeaderAccessor#getSessionId() sessionId}
	 * associated with the STOMP connection.
	 */
	String getSessionId();

	/**
	 * Return the headers that will be sent in the STOMP CONNECT frame.
	 */
	StompHeaderAccessor getConnectHeaders();

}
