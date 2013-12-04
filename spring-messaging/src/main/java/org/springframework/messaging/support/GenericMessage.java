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

package org.springframework.messaging.support;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An implementation of {@link Message} with a generic payload.
 * Once created, a GenericMessage is immutable.
 *
 * @author Mark Fisher
 * @since 4.0
 *
 * @see MessageBuilder
 */
public class GenericMessage<T> implements Message<T>, Serializable {

	private static final long serialVersionUID = 4268801052358035098L;


	private final T payload;

	private final MessageHeaders headers;


	/**
	 * Create a new message with the given payload.
	 *
	 * @param payload the message payload, never {@code null}
	 */
	public GenericMessage(T payload) {
		this(payload, null);
	}

	/**
	 * Create a new message with the given payload and headers.
	 *
	 * @param payload the message payload, never {@code null}
	 * @param headers message headers
	 */
	public GenericMessage(T payload, Map<String, Object> headers) {
		Assert.notNull(payload, "payload must not be null");
		if (headers == null) {
			headers = new HashMap<String, Object>();
		}
		else {
			headers = new HashMap<String, Object>(headers);
		}
		this.headers = new MessageHeaders(headers);
		this.payload = payload;
	}


	public MessageHeaders getHeaders() {
		return this.headers;
	}

	public T getPayload() {
		return this.payload;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.payload instanceof byte[]) {
			sb.append("[Payload byte[").append(((byte[]) this.payload).length).append("]]");
		}
		else {
			sb.append("[Payload ").append(this.payload.getClass().getSimpleName());
			sb.append(" content=").append(this.payload).append("]");
		}
		sb.append("[Headers=").append(this.headers).append("]");
		return sb.toString();
	}

	public int hashCode() {
		return this.headers.hashCode() * 23 + ObjectUtils.nullSafeHashCode(this.payload);
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof GenericMessage<?>) {
			GenericMessage<?> other = (GenericMessage<?>) obj;
			return (this.headers.getId().equals(other.headers.getId()) &&
					this.headers.equals(other.headers) && this.payload.equals(other.payload));
		}
		return false;
	}

}
