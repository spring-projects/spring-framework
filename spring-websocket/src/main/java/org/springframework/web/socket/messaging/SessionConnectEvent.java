/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Event raised when a new WebSocket client using a Simple Messaging Protocol
 * (e.g. STOMP) as the WebSocket sub-protocol issues a connect request.
 *
 * <p>Note that this is not the same as the WebSocket session getting established
 * but rather the client's first attempt to connect within the the sub-protocol,
 * for example sending the STOMP CONNECT frame.
 *
 * <p>The provided {@link #getMessage() message} can be examined to check
 * information about the connected user, The session id, and any headers
 * sent by the client, for STOMP check the class
 * {@link org.springframework.messaging.simp.stomp.StompHeaderAccessor}.
 * For example:
 *
 * <pre class="code">
 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
 * headers.getSessionId();
 * headers.getSessionAttributes();
 * headers.getPrincipal();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectEvent extends ApplicationEvent {

	private final Message<byte[]> message;


	/**
	 * Create a new SessionConnectEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connect message
	 */
	public SessionConnectEvent(Object source, Message<byte[]> message) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
	}


	/**
	 * Return the connect message.
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return "SessionConnectEvent: message=" + this.message;
	}

}
