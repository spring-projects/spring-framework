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
 * A connected event represents the server response to a client's connect request.
 * See {@link org.springframework.web.socket.messaging.SessionConnectEvent}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class SessionConnectedEvent extends ApplicationEvent {

	private final Message<byte[]> message;


	/**
	 * Create a new SessionConnectedEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param message the connected message
	 */
	public SessionConnectedEvent(Object source, Message<byte[]> message) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
	}


	/**
	 * Return the connected message.
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return "SessionConnectedEvent: message=" + this.message;
	}

}
