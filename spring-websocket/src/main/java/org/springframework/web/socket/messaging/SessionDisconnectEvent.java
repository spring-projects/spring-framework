/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;

/**
 * Event raised when the session of a WebSocket client using a Simple Messaging
 * Protocol (for example, STOMP) as the WebSocket sub-protocol is closed.
 *
 * <p>Note that this event may be raised more than once for a single session and
 * therefore event consumers should be idempotent and ignore a duplicate event.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionDisconnectEvent extends AbstractSubProtocolEvent {

	private final String sessionId;

	private final CloseStatus status;


	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message (never {@code null})
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus) {

		this(source, message, sessionId, closeStatus, null);
	}

	/**
	 * Create a new SessionDisconnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the message (never {@code null})
	 * @param sessionId the disconnect message
	 * @param closeStatus the status object
	 * @param user the current session user
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus, @Nullable Principal user) {

		super(source, message, user);
		Assert.notNull(sessionId, "Session id must not be null");
		this.sessionId = sessionId;
		this.status = closeStatus;
	}


	/**
	 * Return the session id.
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Return the status with which the session was closed.
	 */
	public CloseStatus getCloseStatus() {
		return this.status;
	}


	@Override
	public String toString() {
		return "SessionDisconnectEvent[sessionId=" + this.sessionId + ", " + this.status + "]";
	}

}
