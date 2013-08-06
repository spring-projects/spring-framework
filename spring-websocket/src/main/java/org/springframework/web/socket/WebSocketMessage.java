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

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A message that can be handled or sent on a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see BinaryMessage
 * @see TextMessage
 */
public abstract class WebSocketMessage<T> {

	private final T payload;

	private final boolean last;


	/**
	 * Create a new {@link WebSocketMessage} instance with the given payload.
	 * @param payload a non-null payload
	 */
	WebSocketMessage(T payload, boolean isLast) {
		Assert.notNull(payload, "Payload must not be null");
		this.payload = payload;
		this.last = isLast;
	}


	/**
	 * Returns the message payload. This will never be {@code null}.
	 */
	public T getPayload() {
		return this.payload;
	}

	/**
	 * Whether this is the last part of a message, when partial message support on a
	 * {@link WebSocketHandler} is enabled. If partial message support is not enabled the
	 * returned value is always {@code true}.
	 */
	public boolean isLast() {
		return this.last;
	}

	@Override
	public int hashCode() {
		return WebSocketMessage.class.hashCode() * 13 + ObjectUtils.nullSafeHashCode(this.payload);
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
		return ObjectUtils.nullSafeEquals(this.payload, otherMessage.payload);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " payload= " + toStringPayload()
				+ ", length=" + getPayloadSize() + ", last=" + isLast() + "]";
	}

	protected abstract String toStringPayload();

	protected abstract int getPayloadSize();

}
