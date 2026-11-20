/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import org.jspecify.annotations.Nullable;

/**
 * A contract for client STOMP session lifecycle events including a callback
 * when the session is established and notifications of transport or message
 * handling failures.
 *
 * <p>This contract also extends {@link StompFrameHandler} in order to handle
 * STOMP ERROR frames received from the broker.
 *
 * <p>Implementations of this interface should consider extending
 * {@link StompSessionHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see StompSessionHandlerAdapter
 */
public interface StompSessionHandler extends StompFrameHandler {

	/**
	 * Invoked when the session is ready to use, i.e. after the underlying
	 * transport (TCP, WebSocket) is connected and a STOMP CONNECTED frame is
	 * received from the broker.
	 * @param session the client STOMP session
	 * @param connectedHeaders the STOMP CONNECTED frame headers
	 */
	void afterConnected(StompSession session, StompHeaders connectedHeaders);

	/**
	 * Handle any exception arising while processing a STOMP frame such as a
	 * failure to convert the payload or an unhandled exception in the
	 * application {@code StompFrameHandler}.
	 * @param session the client STOMP session
	 * @param command the STOMP command of the frame
	 * @param headers the headers
	 * @param payload the raw payload
	 * @param exception the exception
	 */
	void handleException(StompSession session, @Nullable StompCommand command,
			StompHeaders headers, byte[] payload, Throwable exception);

	/**
	 * Handle a low level transport error which could be an I/O error or a
	 * failure to encode or decode a STOMP message.
	 * <p>Note that
	 * {@link org.springframework.messaging.simp.stomp.ConnectionLostException
	 * ConnectionLostException} will be passed into this method when the
	 * connection is lost rather than closed normally via
	 * {@link StompSession#disconnect()}.
	 * @param session the client STOMP session
	 * @param exception the exception that occurred
	 */
	void handleTransportError(StompSession session, Throwable exception);

}
