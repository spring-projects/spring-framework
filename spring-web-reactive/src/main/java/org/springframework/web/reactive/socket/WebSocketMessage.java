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

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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
	 * Constructor for a WebSocketMessage. To create, see factory methods:
	 * <ul>
	 * <li>{@link WebSocketSession#textMessage}
	 * <li>{@link WebSocketSession#binaryMessage}
	 * <li>{@link WebSocketSession#pingMessage}
	 * <li>{@link WebSocketSession#pongMessage}
	 * </ul>
	 * <p>Alternatively use {@link WebSocketSession#bufferFactory()} to create
	 * the payload and then invoke this constructor.
	 */
	public WebSocketMessage(Type type, DataBuffer payload) {
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

	/**
	 * Return the message payload as UTF-8 text. This is a useful for text
	 * WebSocket messages.
	 */
	public String getPayloadAsText() {
		byte[] bytes = new byte[this.payload.readableByteCount()];
		this.payload.read(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * Retain the data buffer for the message payload, which is useful on
	 * runtimes with pooled buffers, e.g. Netty. A shortcut for:
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.retain(payload);
	 * </pre>
	 * @see DataBufferUtils#retain(DataBuffer)
	 */
	public WebSocketMessage retain() {
		DataBufferUtils.retain(this.payload);
		return this;
	}

	/**
	 * Release the payload {@code DataBuffer} which is useful on runtimes with
	 * pooled buffers such as Netty. Effectively a shortcut for:
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.release(payload);
	 * </pre>
	 * @see DataBufferUtils#release(DataBuffer)
	 */
	public void release() {
		DataBufferUtils.release(this.payload);
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
	 * WebSocket message types.
	 */
	public enum Type { TEXT, BINARY, PING, PONG }

}
