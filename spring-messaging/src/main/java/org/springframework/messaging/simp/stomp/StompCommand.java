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
 * @author Juergen Hoeller
 * @since 4.0
 */
public enum StompCommand {

	// client
	STOMP(SimpMessageType.CONNECT),
	CONNECT(SimpMessageType.CONNECT),
	DISCONNECT(SimpMessageType.DISCONNECT),
	SUBSCRIBE(SimpMessageType.SUBSCRIBE, true, true, false),
	UNSUBSCRIBE(SimpMessageType.UNSUBSCRIBE, false, true, false),
	SEND(SimpMessageType.MESSAGE, true, false, true),
	ACK(SimpMessageType.OTHER),
	NACK(SimpMessageType.OTHER),
	BEGIN(SimpMessageType.OTHER),
	COMMIT(SimpMessageType.OTHER),
	ABORT(SimpMessageType.OTHER),

	// server
	CONNECTED(SimpMessageType.OTHER),
	RECEIPT(SimpMessageType.OTHER),
	MESSAGE(SimpMessageType.MESSAGE, true, true, true),
	ERROR(SimpMessageType.OTHER, false, false, true);


	private final SimpMessageType messageType;

	private final boolean destination;

	private final boolean subscriptionId;

	private final boolean body;


	StompCommand(SimpMessageType messageType) {
		this(messageType, false, false, false);
	}

	StompCommand(SimpMessageType messageType, boolean destination, boolean subscriptionId, boolean body) {
		this.messageType = messageType;
		this.destination = destination;
		this.subscriptionId = subscriptionId;
		this.body = body;
	}


	public SimpMessageType getMessageType() {
		return this.messageType;
	}

	public boolean requiresDestination() {
		return this.destination;
	}

	public boolean requiresSubscriptionId() {
		return this.subscriptionId;
	}

	public boolean requiresContentLength() {
		return this.body;
	}

	public boolean isBodyAllowed() {
		return this.body;
	}

}
