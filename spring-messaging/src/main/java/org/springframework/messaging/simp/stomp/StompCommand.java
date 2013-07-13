/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessageType;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public enum StompCommand {

	// client
	CONNECT,
	STOMP,
	SEND,
	SUBSCRIBE,
	UNSUBSCRIBE,
	ACK,
	NACK,
	BEGIN,
	COMMIT,
	ABORT,
	DISCONNECT,

	// server
	CONNECTED,
	MESSAGE,
	RECEIPT,
	ERROR;


	private static Map<StompCommand, SimpMessageType> messageTypeLookup = new HashMap<StompCommand, SimpMessageType>();

	private static Set<StompCommand> destinationRequiredLookup =
			new HashSet<StompCommand>(Arrays.asList(SEND, SUBSCRIBE, MESSAGE));

	static {
		messageTypeLookup.put(StompCommand.CONNECT, SimpMessageType.CONNECT);
		messageTypeLookup.put(StompCommand.STOMP, SimpMessageType.CONNECT);
		messageTypeLookup.put(StompCommand.SEND, SimpMessageType.MESSAGE);
		messageTypeLookup.put(StompCommand.MESSAGE, SimpMessageType.MESSAGE);
		messageTypeLookup.put(StompCommand.SUBSCRIBE, SimpMessageType.SUBSCRIBE);
		messageTypeLookup.put(StompCommand.UNSUBSCRIBE, SimpMessageType.UNSUBSCRIBE);
		messageTypeLookup.put(StompCommand.DISCONNECT, SimpMessageType.DISCONNECT);
	}

	public SimpMessageType getMessageType() {
		SimpMessageType type = messageTypeLookup.get(this);
		return (type != null) ? type : SimpMessageType.OTHER;
	}

	public boolean requiresDestination() {
		return destinationRequiredLookup.contains(this);
	}

}
