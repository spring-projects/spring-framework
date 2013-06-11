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

package org.springframework.web.messaging.stomp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.messaging.MessageType;


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


	private static Map<StompCommand, MessageType> commandToMessageType = new HashMap<StompCommand, MessageType>();

	static {
		commandToMessageType.put(StompCommand.CONNECT, MessageType.CONNECT);
		commandToMessageType.put(StompCommand.STOMP, MessageType.CONNECT);
		commandToMessageType.put(StompCommand.SEND, MessageType.MESSAGE);
		commandToMessageType.put(StompCommand.SUBSCRIBE, MessageType.SUBSCRIBE);
		commandToMessageType.put(StompCommand.UNSUBSCRIBE, MessageType.UNSUBSCRIBE);
		commandToMessageType.put(StompCommand.DISCONNECT, MessageType.DISCONNECT);
	}

	public MessageType getMessageType() {
		MessageType messageType = commandToMessageType.get(this);
		return (messageType != null) ? messageType : MessageType.OTHER;
	}

}
