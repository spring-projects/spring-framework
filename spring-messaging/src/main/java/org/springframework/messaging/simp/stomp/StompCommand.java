/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
