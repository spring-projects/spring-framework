/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.socket;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Representation of a WebSocket message.
 *
 * <p>See static factory methods in {@link WebSocketSession} for creating messages with
 * the {@link org.springframework.core.io.buffer.DataBufferFactory} for the session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketMessage {

	private static final boolean reactorNetty2Present = ClassUtils.isPresent(
			"io.netty5.handler.codec.http.websocketx.WebSocketFrame", WebSocketMessage.class.getClassLoader());


	private final Type type;

	private final DataBuffer payload;

	@Nullable
	private final Object nativeMessage;


	/**
	 * Constructor for a WebSocketMessage.
	 * <p>See static factory methods in {@link WebSocketSession} or alternatively
	 * use {@link WebSocketSession#bufferFactory()} to create the payload and
	 * then invoke this constructor.
	 */
	public WebSocketMessage(Type type, DataBuffer payload) {
		this(type, payload, null);
	}

	/**
	 * Constructor for an inbound message with access to the underlying message.
	 * @param type the type of WebSocket message
	 * @param payload the message content
	 * @param nativeMessage the message from the API of the underlying WebSocket
	 * library, if applicable.
	 * @since 5.3
	 */
	public WebSocketMessage(Type type, DataBuffer payload, @Nullable Object nativeMessage) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(payload, "'payload' must not be null");
		this.type = type;
		this.payload = payload;
		this.nativeMessage = nativeMessage;
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
	 * Return the message from the API of the underlying WebSocket library. This
	 * is applicable for inbound messages only and when the underlying message
	 * has additional fields other than the content. Currently this is the case
	 * for Reactor Netty only.
	 * @param <T> the type to cast the underlying message to
	 * @return the underlying message, or {@code null}
	 * @since 5.3
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getNativeMessage() {
		return (T) this.nativeMessage;
	}

	/**
	 * A variant of {@link #getPayloadAsText(Charset)} that uses {@code UTF-8}
	 * for decoding the raw content to text.
	 */
	public String getPayloadAsText() {
		return getPayloadAsText(StandardCharsets.UTF_8);
	}

	/**
	 * A shortcut for decoding the raw content of the message to text with the
	 * given character encoding. This is useful for text WebSocket messages, or
	 * otherwise when the payload is expected to contain text.
	 * @param charset the character encoding
	 * @since 5.0.5
	 */
	public String getPayloadAsText(Charset charset) {
		return this.payload.toString(charset);
	}

	/**
	 * Retain the data buffer for the message payload, which is useful on
	 * runtimes (e.g. Netty) with pooled buffers. A shortcut for:
	 * <pre>
	 * DataBuffer payload = message.getPayload();
	 * DataBufferUtils.retain(payload);
	 * </pre>
	 * @see DataBufferUtils#retain(DataBuffer)
	 */
	public WebSocketMessage retain() {
		if (reactorNetty2Present) {
			return ReactorNetty2Helper.retain(this);
		}
		DataBufferUtils.retain(this.payload);
		return this;
	}

	/**
	 * Release the payload {@code DataBuffer} which is useful on runtimes
	 * (e.g. Netty) with pooled buffers such as Netty. A shortcut for:
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
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof WebSocketMessage that &&
				this.type.equals(that.type) &&
				ObjectUtils.nullSafeEquals(this.payload, that.payload)));
	}

	@Override
	public int hashCode() {
		return this.type.hashCode() * 29 + this.payload.hashCode();
	}

	@Override
	public String toString() {
		return "WebSocket " + this.type.name() + " message (" + this.payload.readableByteCount() + " bytes)";
	}


	/**
	 * WebSocket message types.
	 */
	public enum Type {
		/**
		 * Text WebSocket message.
		 */
		TEXT,
		/**
		 * Binary WebSocket message.
		 */
		BINARY,
		/**
		 * WebSocket ping.
		 */
		PING,
		/**
		 * WebSocket pong.
		 */
		PONG
	}


	private static class ReactorNetty2Helper {

		static WebSocketMessage retain(WebSocketMessage message) {
			if (message.nativeMessage instanceof io.netty5.handler.codec.http.websocketx.WebSocketFrame netty5Frame) {
				io.netty5.handler.codec.http.websocketx.WebSocketFrame frame = netty5Frame.send().receive();
				DataBuffer payload = ((Netty5DataBufferFactory) message.payload.factory()).wrap(frame.binaryData());
				return new WebSocketMessage(message.type, payload, frame);
			}
			else {
				DataBufferUtils.retain(message.payload);
				return message;
			}
		}
	}

}
