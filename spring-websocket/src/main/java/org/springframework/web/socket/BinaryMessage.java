/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.web.socket;

import java.nio.ByteBuffer;


/**
 * A {@link WebSocketMessage} that contains a binary {@link ByteBuffer} payload.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class BinaryMessage extends WebSocketMessage<ByteBuffer> {

	private byte[] bytes;


	/**
	 * Create a new {@link BinaryMessage} instance.
	 * @param payload a non-null payload
	 */
	public BinaryMessage(ByteBuffer payload) {
		this(payload, true);
	}

	/**
	 * Create a new {@link BinaryMessage} instance.
	 * @param payload a non-null payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(ByteBuffer payload, boolean isLast) {
		super(payload, isLast);
		this.bytes = null;
	}

	/**
	 * Create a new {@link BinaryMessage} instance.
	 * @param payload a non-null payload
	 */
	public BinaryMessage(byte[] payload) {
		this(payload, 0, (payload == null ? 0 : payload.length), true);
	}

	/**
	 * Create a new {@link BinaryMessage} instance.
	 * @param payload a non-null payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(byte[] payload, boolean isLast) {
		this(payload, 0, (payload == null ? 0 : payload.length), isLast);
	}

	/**
	 * Create a new {@link BinaryMessage} instance by wrapping an existing byte array.
	 * @param payload a non-null payload, NOTE: this value is not copied so care must be
	 *        taken not to modify the array.
	 * @param offset the offet into the array where the payload starts
	 * @param len the length of the array considered for the payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	public BinaryMessage(byte[] payload, int offset, int len, boolean isLast) {
		super(payload != null ? ByteBuffer.wrap(payload, offset, len) : null, isLast);
		if(offset == 0 && len == payload.length) {
			this.bytes = payload;
		}
	}

	/**
	 * Returns access to the message payload as a byte array. NOTE: the returned array
	 * should be considered read-only and should not be modified.
	 */
	public byte[] getByteArray() {
		if(this.bytes == null && getPayload() != null) {
			this.bytes = getRemainingBytes(getPayload());
		}
		return this.bytes;
	}

	private byte[] getRemainingBytes(ByteBuffer payload) {
		byte[] result = new byte[getPayload().remaining()];
		getPayload().get(result);
		return result;
	}

	@Override
	protected int getPayloadSize() {
		return (getPayload() != null) ? getPayload().remaining() : 0;
	}

	@Override
	protected String toStringPayload() {
		return (getPayload() != null) ? getPayload().toString() : null;
	}

}
