/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * A base for classes providing strongly typed getters and setters as well as
 * behavior around specific categories of headers (e.g. STOMP headers).
 * Supports creating new headers, modifying existing headers (when still mutable),
 * or copying and modifying existing headers.
 *
 * <p>The method {@link #getMessageHeaders()} provides access to the underlying,
 * fully-prepared {@link MessageHeaders} that can then be used as-is (i.e.
 * without copying) to create a single message as follows:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.set("foo", "bar");
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 * </pre>
 *
 * <p>After the above, by default the {@code MessageHeaderAccessor} becomes
 * immutable. However it is possible to leave it mutable for further initialization
 * in the same thread, for example:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.set("foo", "bar");
 * accessor.setLeaveMutable(true);
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 *
 * // later on in the same thread...
 *
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * accessor.set("bar", "baz");
 * accessor.setImmutable();
 * </pre>
 *
 * <p>The method {@link #toMap()} returns a copy of the underlying headers. It can
 * be used to prepare multiple messages from the same {@code MessageHeaderAccessor}
 * instance:
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * MessageBuilder builder = MessageBuilder.withPayload("payload").setHeaders(accessor);
 *
 * accessor.setHeader("foo", "bar1");
 * Message message1 = builder.build();
 *
 * accessor.setHeader("foo", "bar2");
 * Message message2 = builder.build();
 *
 * accessor.setHeader("foo", "bar3");
 * Message  message3 = builder.build();
 * </pre>
 *
 * <p>However note that with the above style, the header accessor is shared and
 * cannot be re-obtained later on. Alternatively it is also possible to create
 * one {@code MessageHeaderAccessor} per message:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor1 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar1");
 * Message message1 = MessageBuilder.createMessage("payload", accessor1.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor2 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar2");
 * Message message2 = MessageBuilder.createMessage("payload", accessor2.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor3 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar3");
 * Message message3 = MessageBuilder.createMessage("payload", accessor3.getMessageHeaders());
 * </pre>
 *
 * <p>Note that the above examples aim to demonstrate the general idea of using
 * header accessors. The most likely usage however is through sub-classes.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageHeaderAccessor {

	protected Log logger = LogFactory.getLog(getClass());

	private final MutableMessageHeaders headers;

	private boolean modified;

	private boolean leaveMutable;

	private IdGenerator idGenerator;

	private boolean enableTimestamp = false;


	/**
	 * A constructor to create new headers.
	 */
	public MessageHeaderAccessor() {
		this.headers = new MutableMessageHeaders();
	}

	/**
	 * A constructor accepting the headers of an existing message to copy.
	 */
	public MessageHeaderAccessor(Message<?> message) {
		if (message != null) {
			this.headers = new MutableMessageHeaders(message.getHeaders());
		}
		else {
			this.headers = new MutableMessageHeaders();
		}
	}

	/**
	 * Return the original {@code MessageHeaderAccessor} used to create the headers
	 * of the given {@code Message}, or {@code null} if that's not available or if
	 * its type does not match the required type.
	 *
	 * <p>This is for cases where the existence of an accessor is strongly expected
	 * (to be followed up with an assertion) or will created if not provided.
	 *
	 * @return an accessor instance of the specified type or {@code null}.
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T extends MessageHeaderAccessor> T getAccessor(Message<?> message, Class<T> requiredType) {
		return getAccessor(message.getHeaders(), requiredType);
	}

	/**
	 * A variation of {@link #getAccessor(org.springframework.messaging.Message, Class)}
	 * with a {@code MessageHeaders} instance instead of a {@code Message}.
	 *
	 * <p>This is for cases when a full message may not have been created yet.
	 *
	 * @return an accessor instance of the specified type or {@code null}.
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T extends MessageHeaderAccessor> T getAccessor(MessageHeaders messageHeaders, Class<T> requiredType) {
		if (messageHeaders instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) messageHeaders;
			MessageHeaderAccessor headerAccessor = mutableHeaders.getMessageHeaderAccessor();
			if (requiredType.isAssignableFrom(headerAccessor.getClass()))  {
				return (T) headerAccessor;
			}
		}
		return null;
	}

	/**
	 * Return a mutable {@code MessageHeaderAccessor} for the given message attempting
	 * to match the type of accessor used to create the message headers, or otherwise
	 * wrapping the message with a {@code MessageHeaderAccessor} instance.
	 *
	 * <p>This is for cases where a header needs to be updated in generic code
	 * while preserving the accessor type for downstream processing.
	 *
	 * @return an accessor of the required type, never {@code null}.
	 * @since 4.1
	 */
	public static MessageHeaderAccessor getMutableAccessor(Message<?> message) {
		if (message.getHeaders() instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) message.getHeaders();
			MessageHeaderAccessor accessor = mutableHeaders.getMessageHeaderAccessor();
			if (accessor != null) {
				return (accessor.isMutable() ? accessor : accessor.createAccessor(message));
			}
		}
		return new MessageHeaderAccessor(message);
	}

	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return new MessageHeaderAccessor(message);
	}

	/**
	 * Return the underlying {@code MessageHeaders} instance.
	 *
	 * <p>Unless {@link #setLeaveMutable(boolean)} was set to {@code true}, after
	 * this call, the headers are immutable and this accessor can no longer
	 * modify them.
	 *
	 * <p>This method always returns the same {@code MessageHeaders} instance if
	 * invoked multiples times. To obtain a copy of the underlying headers instead
	 * use {@link #toMap()}.
	 */
	public MessageHeaders getMessageHeaders() {
		if (!this.leaveMutable) {
			setImmutable();
		}
		return this.headers;
	}

	/**
	 * Return a copy of the underlying header values.
	 *
	 * <p>This method can be invoked many times, with modifications in between
	 * where each new call returns a fresh copy of the current header values.
	 */
	public Map<String, Object> toMap() {
		return new HashMap<String, Object>(this.headers);
	}

	/**
	 * By default when {@link #getMessageHeaders()} is called, {@code "this"}
	 * {@code MessageHeaderAccessor} instance can no longer be used to modify the
	 * underlying message headers and the returned {@code MessageHeaders} is immutable.
	 *
	 * <p>However when this is set to {@code true}, the returned (underlying)
	 * {@code MessageHeaders} instance remains mutable. To make further modifications
	 * continue to use the same accessor instance or re-obtain it via:<br>
	 * {@link org.springframework.messaging.support.MessageHeaderAccessor#getAccessor(org.springframework.messaging.Message, Class)
	 * MessageHeaderAccessor.getAccessor(Message, Class)}
	 *
	 * <p>When modifications are complete use {@link #setImmutable()} to prevent
	 * further changes. The intended use case for this mechanism is initialization
	 * of a Message within a single thread.
	 *
	 * <p>By default this is set to {@code false}.
	 * @since 4.1
	 */
	public void setLeaveMutable(boolean leaveMutable) {
		Assert.state(this.headers.isMutable(), "Already immutable");
		this.leaveMutable = leaveMutable;
	}

	/**
	 * By default when {@link #getMessageHeaders()} is called, {@code "this"}
	 * {@code MessageHeaderAccessor} instance can no longer be used to modify the
	 * underlying message headers. However if {@link #setLeaveMutable(boolean)}
	 * is used, this method is necessary to indicate explicitly when the
	 * {@code MessageHeaders} instance should no longer be modified.
	 * @since 4.1
	 */
	public void setImmutable() {
		this.headers.setIdAndTimestamp();
		this.headers.setImmutable();
	}

	/**
	 * Whether the underlying headers can still be modified.
	 * @since 4.1
	 */
	public boolean isMutable() {
		return this.headers.isMutable();
	}

	/**
	 * A package private mechanism to configure the IdGenerator strategy to use.
	 *
	 * <p>By default this property is not set in which case the default IdGenerator
	 * in {@link org.springframework.messaging.MessageHeaders} is used.
	 *
	 * @see org.springframework.messaging.support.IdTimestampMessageHeaderInitializer
	 */
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	/**
	 * A package private mechanism to enables the automatic addition of the
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header.
	 *
	 * <p>By default this property is set to false.
	 *
	 * @see org.springframework.messaging.support.IdTimestampMessageHeaderInitializer
	 */
	void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	public boolean isModified() {
		return this.modified;
	}

	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	public Object getHeader(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * Set the value for the given header name. If the provided value is {@code null} the
	 * header will be removed.
	 */
	public void setHeader(String name, Object value) {
		Assert.isTrue(!isReadOnly(name), "The '" + name + "' header is read-only.");
		verifyType(name, value);
		if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
			this.modified = true;
			if (value != null) {
				this.headers.getRawHeaders().put(name, value);
			}
			else {
				this.headers.getRawHeaders().remove(name);
			}
		}
	}

	protected void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ERROR_CHANNEL.equals(headerName) || MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				Assert.isTrue(headerValue instanceof MessageChannel || headerValue instanceof String, "The '"
						+ headerName + "' header value must be a MessageChannel or String.");
			}
		}
	}

	protected boolean isReadOnly(String headerName) {
		return MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName);
	}

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 */
	public void setHeaderIfAbsent(String name, Object value) {
		if (getHeader(name) == null) {
			setHeader(name, value);
		}
	}

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests
	 * the array may contain simple matching patterns for header names. Supported pattern
	 * styles are: "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 */
	public void removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<String>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
					headersToRemove.addAll(getMatchingHeaderNames(pattern, this.headers));
				}
				else {
					headersToRemove.add(pattern);
				}
			}
		}
		for (String headerToRemove : headersToRemove) {
			removeHeader(headerToRemove);
		}
	}

	private List<String> getMatchingHeaderNames(String pattern, Map<String, Object> headers) {
		List<String> matchingHeaderNames = new ArrayList<String>();
		if (headers != null) {
			for (Map.Entry<String, Object> header: headers.entrySet()) {
				if (PatternMatchUtils.simpleMatch(pattern,  header.getKey())) {
					matchingHeaderNames.add(header.getKey());
				}
			}
		}
		return matchingHeaderNames;
	}

	/**
	 * Remove the value for the given header name.
	 */
	public void removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !isReadOnly(headerName)) {
			setHeader(headerName, null);
		}
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any
	 * existing values. Use { {@link #copyHeadersIfAbsent(Map)} to avoid overwriting
	 * values.
	 */
	public void copyHeaders(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			Set<String> keys = headersToCopy.keySet();
			for (String key : keys) {
				if (!isReadOnly(key)) {
					setHeader(key, headersToCopy.get(key));
				}
			}
		}
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em>
	 * overwrite any existing values.
	 */
	public void copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			Set<String> keys = headersToCopy.keySet();
			for (String key : keys) {
				if (!this.isReadOnly(key)) {
					setHeaderIfAbsent(key, headersToCopy.get(key));
				}
			}
		}
	}

	public UUID getId() {
		return (UUID) getHeader(MessageHeaders.ID);
	}

	public Long getTimestamp() {
		return (Long) getHeader(MessageHeaders.TIMESTAMP);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public Object getReplyChannel() {
        return getHeader(MessageHeaders.REPLY_CHANNEL);
    }

	public void setReplyChannelName(String replyChannelName) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

    public Object getErrorChannel() {
        return getHeader(MessageHeaders.ERROR_CHANNEL);
    }

	public void setErrorChannelName(String errorChannelName) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

    public MimeType getContentType() {
        return (MimeType) getHeader(MessageHeaders.CONTENT_TYPE);
    }

	public void setContentType(MimeType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [headers=" + this.headers + "]";
	}


	@SuppressWarnings("serial")
	private class MutableMessageHeaders extends MessageHeaders {

		private boolean immutable;


		public MutableMessageHeaders() {
			this(null);
		}

		public MutableMessageHeaders(Map<String, Object> headers) {
			super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
		}

		public MessageHeaderAccessor getMessageHeaderAccessor() {
			return MessageHeaderAccessor.this;
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			Assert.state(!this.immutable, "Already immutable");
			return super.getRawHeaders();
		}

		public void setImmutable() {
			this.immutable = true;
		}

		public boolean isMutable() {
			return !this.immutable;
		}

		public void setIdAndTimestamp() {
			if (!isMutable()) {
				return;
			}
			if (getId() == null) {
				IdGenerator idGenerator = (MessageHeaderAccessor.this.idGenerator != null) ?
						MessageHeaderAccessor.this.idGenerator :
						MessageHeaders.getIdGenerator();

				UUID id = idGenerator.generateId();
				if (id != null && id != MessageHeaders.ID_VALUE_NONE) {
					getRawHeaders().put(ID, id);
				}
			}
			if (getTimestamp() == null) {
				if (MessageHeaderAccessor.this.enableTimestamp) {
					getRawHeaders().put(TIMESTAMP, System.currentTimeMillis());
				}
			}
		}
	}

}
