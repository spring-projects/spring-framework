/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * A connected event represents the server response to a client's connect request.
 * See {@link org.springframework.web.socket.messaging.SessionConnectEvent}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectedEvent extends AbstractSubProtocolEvent {

	/**
	 * Create a new SessionConnectedEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connected message (never {@code null})
	 */
	public SessionConnectedEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionConnectedEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source, message, user);
	}

}
