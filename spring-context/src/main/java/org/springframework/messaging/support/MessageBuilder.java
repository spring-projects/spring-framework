/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @since 4.0
 */
public final class MessageBuilder<T> {

	private final T payload;

	private final Map<String, Object> headers = new HashMap<String, Object>();

	private final Message<T> originalMessage;

	@SuppressWarnings("rawtypes")
	private static volatile MessageFactory messageFactory = null;


	/**
	 * A constructor with payload and an optional message to copy headers from.
	 * This is a private constructor to be invoked from the static factory methods only.
	 *
	 * @param payload the message payload, never {@code null}
	 * @param originalMessage a message to copy from or re-use if no changes are made, can
	 *        be {@code null}
	 */
	private MessageBuilder(T payload, Message<T> originalMessage) {
		Assert.notNull(payload, "payload is required");
		this.payload = payload;
		this.originalMessage = originalMessage;
		if (originalMessage != null) {
			this.headers.putAll(originalMessage.getHeaders());
		}
	}

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 *
	 * @param payload the message payload, never {@code null}
	 * @param originalMessage a message to copy from or re-use if no changes are made, can
	 *        be {@code null}
	 */
	private MessageBuilder(T payload, Map<String, Object> headers) {
		Assert.notNull(payload, "payload is required");
		Assert.notNull(headers, "headers is required");
		this.payload = payload;
		this.headers.putAll(headers);
		this.originalMessage = null;
	}

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all of the
	 * headers copied from the provided message. The payload of the provided Message will
	 * also be used as the payload for the new message.
	 *
	 * @param message the Message from which the payload and all headers will be copied
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		MessageBuilder<T> builder = new MessageBuilder<T>(message.getPayload(), message);
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload and
	 * headers.
	 *
	 * @param payload the payload for the new message
	 * @param headers the headers to use
	 */
	public static <T> MessageBuilder<T> fromPayloadAndHeaders(T payload, Map<String, Object> headers) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload, headers);
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 *
	 * @param payload the payload for the new message
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload, (Message<T>) null);
		return builder;
	}

	/**
	 * Set the value for the given header name. If the provided value is <code>null</code>
	 * the header will be removed.
	 */
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		Assert.isTrue(!this.isReadOnly(headerName), "The '" + headerName + "' header is read-only.");
		if (StringUtils.hasLength(headerName)) {
			putOrRemove(headerName, headerValue);
		}
		return this;
	}

	private boolean isReadOnly(String headerName) {
		return MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName);
	}

	private void putOrRemove(String headerName, Object headerValue) {
		if (headerValue == null) {
			this.headers.remove(headerName);
		}
		else {
			this.headers.put(headerName, headerValue);
		}
	}

	/**
	 * Set the value for the given header name only if the header name is not already
	 * associated with a value.
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		if (this.headers.get(headerName) == null) {
			putOrRemove(headerName, headerValue);
		}
		return this;
	}

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests
	 * the array may contain simple matching patterns for header names. Supported pattern
	 * styles are: "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 */
	public MessageBuilder<T> removeHeaders(String... headerPatterns) {
		List<String> toRemove = new ArrayList<String>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
					for (String headerName : this.headers.keySet()) {
						if (PatternMatchUtils.simpleMatch(pattern, headerName)){
							toRemove.add(headerName);
						}
					}
				}
				else {
					toRemove.add(pattern);
				}
			}
		}
		for (String headerName : toRemove) {
			this.headers.remove(headerName);
			putOrRemove(headerName, null);
		}
		return this;
	}
	/**
	 * Remove the value for the given header name.
	 */
	public MessageBuilder<T> removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !isReadOnly(headerName)) {
			this.headers.remove(headerName);
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any
	 * existing values. Use { {@link #copyHeadersIfAbsent(Map)} to avoid overwriting
	 * values. Note that the 'id' and 'timestamp' header values will never be overwritten.
	 */
	public MessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			if (!this.isReadOnly(key)) {
				putOrRemove(key, headersToCopy.get(key));
			}
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em>
	 * overwrite any existing values.
	 */
	public MessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			if (!this.isReadOnly(key) && (this.headers.get(key) == null)) {
				putOrRemove(key, headersToCopy.get(key));
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public Message<T> build() {

		if (this.originalMessage != null
				&& this.headers.equals(this.originalMessage.getHeaders())
				&& this.payload.equals(this.originalMessage.getPayload())) {

			return this.originalMessage;
		}

//		if (this.payload instanceof Throwable) {
//			return (Message<T>) new ErrorMessage((Throwable) this.payload, this.headers);
//		}

		this.headers.remove(MessageHeaders.ID);
		this.headers.remove(MessageHeaders.TIMESTAMP);

		if (messageFactory == null) {
			return new GenericMessage<T>(this.payload, this.headers);
		}
		else {
			return messageFactory.createMessage(payload, headers);
		}
	}

}
