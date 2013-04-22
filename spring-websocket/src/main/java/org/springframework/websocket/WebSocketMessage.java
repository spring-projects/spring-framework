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

import org.springframework.util.ObjectUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class WebSocketMessage<T> {

	private final T payload;


	WebSocketMessage(T payload) {
		this.payload = payload;
	}

	public T getPayload() {
		return this.payload;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [payload=" + this.payload + "]";
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

}
