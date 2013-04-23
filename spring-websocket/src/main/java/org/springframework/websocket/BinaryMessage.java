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
package org.springframework.websocket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.springframework.util.Assert;


/**
 * Represents a binary WebSocket message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class BinaryMessage extends WebSocketMessage<ByteBuffer> {

	private byte[] bytes;

	private final boolean last;


	public BinaryMessage(ByteBuffer payload) {
		this(payload, true);
	}

	public BinaryMessage(ByteBuffer payload, boolean isLast) {
		super(payload);
		this.bytes = null;
		this.last = isLast;
	}

	public BinaryMessage(byte[] payload) {
		this(payload, true);
	}

	public BinaryMessage(byte[] payload, boolean isLast) {
		this(payload, 0, (payload == null ? 0 : payload.length), isLast);
	}

	public BinaryMessage(byte[] payload, int offset, int len) {
		this(payload, offset, len, true);
	}

	public BinaryMessage(byte[] payload, int offset, int len, boolean isLast) {
		super(payload != null ? ByteBuffer.wrap(payload, offset, len) : null);
		if(payload != null && offset == 0 && len == payload.length) {
			// FIXME better if a message always needs a payload?
			this.bytes = payload;
		}
		this.last = isLast;
	}

	public boolean isLast() {
		return this.last;
	}

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

	public InputStream getInputStream() {
		byte[] array = getByteArray();
		return (array != null) ? new ByteArrayInputStream(array) : null;
	}

	@Override
	public String toString() {
		int size = (getPayload() != null) ? getPayload().remaining() : 0;
		return "WebSocket binary message size=" + size;
	}

}
