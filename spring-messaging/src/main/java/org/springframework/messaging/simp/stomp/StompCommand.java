/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.simp.SimpMessageType;

/**
 * Represents a STOMP command.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public enum StompCommand {

	// client
	CONNECT(SimpMessageType.CONNECT, 0),
	STOMP(SimpMessageType.CONNECT, 0),
	DISCONNECT(SimpMessageType.DISCONNECT, 0),
	SUBSCRIBE(SimpMessageType.SUBSCRIBE, 3),
	UNSUBSCRIBE(SimpMessageType.UNSUBSCRIBE, 2),
	SEND(SimpMessageType.MESSAGE, 13),
	ACK(SimpMessageType.OTHER, 0),
	NACK(SimpMessageType.OTHER, 0),
	BEGIN(SimpMessageType.OTHER, 0),
	COMMIT(SimpMessageType.OTHER, 0),
	ABORT(SimpMessageType.OTHER, 0),

	// server
	CONNECTED(SimpMessageType.OTHER, 0),
	MESSAGE(SimpMessageType.MESSAGE, 15),
	RECEIPT(SimpMessageType.OTHER, 0),
	ERROR(SimpMessageType.OTHER, 12);

	private static final int DESTINATION_REQUIRED = 1;
	private static final int SUBSCRIPTION_ID_REQUIRED = 2;
	private static final int CONTENT_LENGTH_REQUIRED = 4;
	private static final int BODY_ALLOWED = 8;

	private final SimpMessageType simpMessageType;
	private final int flags;

	StompCommand(final SimpMessageType simpMessageType, final int flags) {
		this.simpMessageType = simpMessageType;
		this.flags = flags;
	}

	public SimpMessageType getMessageType() {
		return simpMessageType;
	}

	public boolean requiresDestination() {
		return (flags & DESTINATION_REQUIRED) != 0;
	}

	public boolean requiresSubscriptionId() {
		return (flags & SUBSCRIPTION_ID_REQUIRED) != 0;
	}

	public boolean requiresContentLength() {
		return (flags & CONTENT_LENGTH_REQUIRED) != 0;
	}

	public boolean isBodyAllowed() {
		return (flags & BODY_ALLOWED) != 0;
	}

}

