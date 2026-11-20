/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * Event raised when a new WebSocket client using a Simple Messaging Protocol
 * (for example, STOMP) as the WebSocket sub-protocol issues a connect request.
 *
 * <p>Note that this is not the same as the WebSocket session getting established
 * but rather the client's first attempt to connect within the sub-protocol,
 * for example sending the STOMP CONNECT frame.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectEvent extends AbstractSubProtocolEvent {

	/**
	 * Create a new SessionConnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connect message
	 */
	public SessionConnectEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source, message, user);
	}

}
