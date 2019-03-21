/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket;

import java.nio.ByteBuffer;

/**
 * A binary WebSocket message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class BinaryMessage extends AbstractWebSocketMessage<ByteBuffer> {

	/**
	 * Create a new binary WebSocket message with the given ByteBuffer payload.
	 * @param payload the non-null payload
	 */
	public BinaryMessage(ByteBuffer payload) {
		super(payload, true);
	}

	/**
	 * Create a new binary WebSocket message with the given payload representing the
	 * full or partial message content. When the {@code isLast} boolean flag is set
	 * to {@code false} the message is sent as partial content and more partial
	 * messages will be expected until the boolean flag is set to {@code true}.
	 * @param payload the non-null payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(ByteBuffer payload, boolean isLast) {
		super(payload, isLast);
	}

	/**
	 * Create a new binary WebSocket message with the given byte[] payload.
	 * @param payload a non-null payload; note that this value is not copied so care
	 * must be taken not to modify the array.
	 */
	public BinaryMessage(byte[] payload) {
		this(payload, true);
	}

	/**
	 * Create a new binary WebSocket message with the given byte[] payload representing
	 * the full or partial message content. When the {@code isLast} boolean flag is set
	 * to {@code false} the message is sent as partial content and more partial
	 * messages will be expected until the boolean flag is set to {@code true}.
	 * @param payload a non-null payload; note that this value is not copied so care
	 * must be taken not to modify the array.
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(byte[] payload, boolean isLast) {
		this(payload, 0, payload.length, isLast);
	}

	/**
	 * Create a new binary WebSocket message by wrapping an existing byte array.
	 * @param payload a non-null payload; note that this value is not copied so care
	 * must be taken not to modify the array.
	 * @param offset the offset into the array where the payload starts
	 * @param length the length of the array considered for the payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(byte[] payload, int offset, int length, boolean isLast) {
		super(ByteBuffer.wrap(payload, offset, length), isLast);
	}


	@Override
	public int getPayloadLength() {
		return getPayload().remaining();
	}

	@Override
	protected String toStringPayload() {
		return getPayload().toString();
	}

}
