/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A base class for events for a message received from a WebSocket client and
 * parsed into a higher-level sub-protocol (for example, STOMP).
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@SuppressWarnings("serial")
public abstract class AbstractSubProtocolEvent extends ApplicationEvent {

	private final Message<byte[]> message;

	private final @Nullable Principal user;


	/**
	 * Create a new AbstractSubProtocolEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message (never {@code null})
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message) {
		this(source, message, null);
	}

	/**
	 * Create a new AbstractSubProtocolEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the incoming message (never {@code null})
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message, @Nullable Principal user) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = user;
	}


	/**
	 * Return the Message associated with the event. Here is an example of
	 * obtaining information about the session id or any headers in the
	 * message:
	 * <pre class="code">
	 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
	 * headers.getSessionId();
	 * headers.getSessionAttributes();
	 * headers.getPrincipal();
	 * </pre>
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	/**
	 * Return the user for the session associated with the event.
	 */
	public @Nullable Principal getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.message + "]";
	}

}
