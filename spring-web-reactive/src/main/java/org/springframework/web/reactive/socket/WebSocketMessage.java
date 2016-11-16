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
package org.springframework.web.reactive.socket;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Representation of a WebSocket message.
 * Use one of the static factory methods in this class to create a message.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketMessage {

	private final Type type;

	private final DataBuffer payload;


	/**
	 * Private constructor. See static factory methods.
	 */
	private WebSocketMessage(Type type, DataBuffer payload) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(payload, "'payload' must not be null");
		this.type = type;
		this.payload = payload;
	}


	/**
	 * Return the message type (text, binary, etc).
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Return the message payload.
	 */
	public DataBuffer getPayload() {
		return this.payload;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WebSocketMessage)) {
			return false;
		}
		WebSocketMessage otherMessage = (WebSocketMessage) other;
		return (this.type.equals(otherMessage.type) &&
				ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload));
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() * 29 + this.payload.hashCode();
	}


	/**
	 * Factory method to create a text WebSocket message.
	 */
	public static WebSocketMessage text(DataBuffer payload) {
		return create(Type.TEXT, payload);
	}

	/**
	 * Factory method to create a binary WebSocket message.
	 */
	public static WebSocketMessage binary(DataBuffer payload) {
		return create(Type.BINARY, payload);
	}

	/**
	 * Factory method to create a ping WebSocket message.
	 */
	public static WebSocketMessage ping(DataBuffer payload) {
		return create(Type.PING, payload);
	}

	/**
	 * Factory method to create a pong WebSocket message.
	 */
	public static WebSocketMessage pong(DataBuffer payload) {
		return create(Type.PONG, payload);
	}

	/**
	 * Factory method to create a WebSocket message of the given type.
	 */
	public static WebSocketMessage create(Type type, DataBuffer payload) {
		return new WebSocketMessage(type, payload);
	}


	/**
	 * WebSocket message types.
	 */
	public enum Type { TEXT, BINARY, PING, PONG }

}
