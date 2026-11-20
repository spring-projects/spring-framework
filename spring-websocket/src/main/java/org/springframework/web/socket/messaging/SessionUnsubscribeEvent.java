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
 * (for example, STOMP) sends a request to remove a subscription.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionUnsubscribeEvent extends AbstractSubProtocolEvent {

	public SessionUnsubscribeEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

	public SessionUnsubscribeEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source, message, user);
	}

}
