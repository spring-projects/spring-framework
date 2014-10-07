/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A message that can be handled or sent on a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketMessage<T> implements WebSocketMessage<T> {

	private final T payload;

	private final boolean last;


	/**
	 * Create a new WebSocket message with the given payload.
	 * @param payload the non-null payload
	 */
	AbstractWebSocketMessage(T payload) {
		this(payload, true);
	}

	/**
	 * Create a new WebSocket message given payload representing the full or partial
	 * message content. When the {@code isLast} boolean flag is set to {@code false}
	 * the message is sent as partial content and more partial messages will be
	 * expected until the boolean flag is set to {@code true}.
	 * @param payload the non-null payload
	 * @param isLast if the message is the last of a series of partial messages
	 */
	AbstractWebSocketMessage(T payload, boolean isLast) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.last = isLast;
	}


	/**
	 * Return the message payload, never be {@code null}.
	 */
	public T getPayload() {
		return this.payload;
	}

	/**
	 * Whether this is the last part of a message sent as a series of partial messages.
	 */
	public boolean isLast() {
		return this.last;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractWebSocketMessage)) {
			return false;
		}
		AbstractWebSocketMessage<?> otherMessage = (AbstractWebSocketMessage<?>) other;
		return ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.payload);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " payload=[" + toStringPayload() +
				"], byteCount=" + getPayloadLength() + ", last=" + isLast() + "]";
	}

	protected abstract String toStringPayload();

}
