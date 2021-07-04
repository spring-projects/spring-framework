/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.support;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Wrapper around {@link MessageHeaders} that provides extra features such as
 * strongly typed accessors for specific headers, the ability to leave headers
 * in a {@link Message} mutable, and the option to suppress automatic generation
 * of {@link MessageHeaders#ID id} and {@link MessageHeaders#TIMESTAMP
 * timesteamp} headers. Sub-classes such as {@link NativeMessageHeaderAccessor}
 * and others provide support for managing processing vs external source headers
 * as well as protocol specific headers.
 *
 * <p>Below is a workflow to initialize headers via {@code MessageHeaderAccessor},
 * or one of its sub-classes, then create a {@link Message}, and then re-obtain
 * the accessor possibly from a different component:
 * <pre class="code">
 * // Create a message with headers
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * MessageHeaders headers = accessor.getMessageHeaders();
 * Message message = MessageBuilder.createMessage("payload", headers);
 *
 * // Later on
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * Assert.notNull(accessor, "No MessageHeaderAccessor");
 * </pre>
 *
 * <p>In order for the above to work, all participating components must use
 * {@code MessageHeaders} to create, access, or modify headers, or otherwise
 * {@link MessageHeaderAccessor#getAccessor(Message, Class)} will return null.
 * Below is a workflow that shows how headers are created and left mutable,
 * then modified possibly by a different component, and finally made immutable
 * perhaps before the possibility of being accessed on a different thread:
 * <pre class="code">
 * // Create a message with mutable headers
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * accessor.setLeaveMutable(true);
 * MessageHeaders headers = accessor.getMessageHeaders();
 * Message message = MessageBuilder.createMessage("payload", headers);
 *
 * // Later on
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * if (accessor.isMutable()) {
 *     // It's mutable, just change the headers
 *     accessor.setHeader("bar", "baz");
 * }
 * else {
 *     // It's not, so get a mutable copy, change and re-create
 *     accessor = MessageHeaderAccessor.getMutableAccessor(message);
 *     accessor.setHeader("bar", "baz");
 *     accessor.setLeaveMutable(true); // leave mutable again or not?
 *     message = MessageBuilder.createMessage(message.getPayload(), accessor);
 * }
 *
 * // Make the accessor immutable
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * accessor.setImmutable();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MessageHeaderAccessor {

	/**
	 * The default charset used for headers.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final MimeType[] READABLE_MIME_TYPES = new MimeType[] {
			MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.APPLICATION_XML,
			new MimeType("text", "*"), new MimeType("application", "*+json"), new MimeType("application", "*+xml")
	};


	private final MutableMessageHeaders headers;

	private boolean leaveMutable = false;

	private boolean modified = false;

	private boolean enableTimestamp = false;

	@Nullable
	private IdGenerator idGenerator;


	/**
	 * A constructor to create new headers.
	 */
	public MessageHeaderAccessor() {
		this(null);
	}

	/**
	 * A constructor accepting the headers of an existing message to copy.
	 * @param message a message to copy the headers from, or {@code null} if none
	 */
	public MessageHeaderAccessor(@Nullable Message<?> message) {
		this.headers = new MutableMessageHeaders(message != null ? message.getHeaders() : null);
	}


	/**
	 * Build a 'nested' accessor for the given message.
	 * @param message the message to build a new accessor for
	 * @return the nested accessor (typically a specific subclass)
	 */
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return new MessageHeaderAccessor(message);
	}


	// Configuration properties

	/**
	 * By default when {@link #getMessageHeaders()} is called, {@code "this"}
	 * {@code MessageHeaderAccessor} instance can no longer be used to modify the
	 * underlying message headers and the returned {@code MessageHeaders} is immutable.
	 * <p>However when this is set to {@code true}, the returned (underlying)
	 * {@code MessageHeaders} instance remains mutable. To make further modifications
	 * continue to use the same accessor instance or re-obtain it via:<br>
	 * {@link MessageHeaderAccessor#getAccessor(Message, Class)
	 * MessageHeaderAccessor.getAccessor(Message, Class)}
	 * <p>When modifications are complete use {@link #setImmutable()} to prevent
	 * further changes. The intended use case for this mechanism is initialization
	 * of a Message within a single thread.
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
	 * Mark the underlying message headers as modified.
	 * @param modified typically {@code true}, or {@code false} to reset the flag
	 * @since 4.1
	 */
	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * Check whether the underlying message headers have been marked as modified.
	 * @return {@code true} if the flag has been set, {@code false} otherwise
	 */
	public boolean isModified() {
		return this.modified;
	}

	/**
	 * A package private mechanism to enables the automatic addition of the
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header.
	 * <p>By default, this property is set to {@code false}.
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * A package-private mechanism to configure the IdGenerator strategy to use.
	 * <p>By default this property is not set in which case the default IdGenerator
	 * in {@link org.springframework.messaging.MessageHeaders} is used.
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}


	// Accessors for the resulting MessageHeaders

	/**
	 * Return the underlying {@code MessageHeaders} instance.
	 * <p>Unless {@link #setLeaveMutable(boolean)} was set to {@code true}, after
	 * this call, the headers are immutable and this accessor can no longer
	 * modify them.
	 * <p>This method always returns the same {@code MessageHeaders} instance if
	 * invoked multiples times. To obtain a copy of the underlying headers, use
	 * {@link #toMessageHeaders()} or {@link #toMap()} instead.
	 * @since 4.1
	 */
	public MessageHeaders getMessageHeaders() {
		if (!this.leaveMutable) {
			setImmutable();
		}
		return this.headers;
	}

	/**
	 * Return a copy of the underlying header values as a {@link MessageHeaders} object.
	 * <p>This method can be invoked many times, with modifications in between
	 * where each new call returns a fresh copy of the current header values.
	 * @since 4.1
	 */
	public MessageHeaders toMessageHeaders() {
		return new MessageHeaders(this.headers);
	}

	/**
	 * Return a copy of the underlying header values as a plain {@link Map} object.
	 * <p>This method can be invoked many times, with modifications in between
	 * where each new call returns a fresh copy of the current header values.
	 */
	public Map<String, Object> toMap() {
		return new HashMap<>(this.headers);
	}


	// Generic header accessors

	/**
	 * Retrieve the value for the header with the given name.
	 * @param headerName the name of the header
	 * @return the associated value, or {@code null} if none found
	 */
	@Nullable
	public Object getHeader(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * Set the value for the given header name.
	 * <p>If the provided value is {@code null}, the header will be removed.
	 */
	public void setHeader(String name, @Nullable Object value) {
		if (isReadOnly(name)) {
			throw new IllegalArgumentException("'" + name + "' header is read-only");
		}
		verifyType(name, value);
		if (value != null) {
			// Modify header if necessary
			if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
				this.modified = true;
				this.headers.getRawHeaders().put(name, value);
			}
		}
		else {
			// Remove header if available
			if (this.headers.containsKey(name)) {
				this.modified = true;
				this.headers.getRawHeaders().remove(name);
			}
		}
	}

	protected void verifyType(@Nullable String headerName, @Nullable Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ERROR_CHANNEL.equals(headerName) ||
					MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				if (!(headerValue instanceof MessageChannel || headerValue instanceof String)) {
					throw new IllegalArgumentException(
							"'" + headerName + "' header value must be a MessageChannel or String");
				}
			}
		}
	}

	/**
	 * Set the value for the given header name only if the header name is not
	 * already associated with a value.
	 */
	public void setHeaderIfAbsent(String name, Object value) {
		if (getHeader(name) == null) {
			setHeader(name, value);
		}
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
	 * Removes all headers provided via array of 'headerPatterns'.
	 * <p>As the name suggests, array may contain simple matching patterns for header
	 * names. Supported pattern styles are: "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 */
	public void removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<>();
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

	private List<String> getMatchingHeaderNames(String pattern, @Nullable Map<String, Object> headers) {
		if (headers == null) {
			return Collections.emptyList();
		}
		List<String> matchingHeaderNames = new ArrayList<>();
		for (String key : headers.keySet()) {
			if (PatternMatchUtils.simpleMatch(pattern, key)) {
				matchingHeaderNames.add(key);
			}
		}
		return matchingHeaderNames;
	}

	/**
	 * Copy the name-value pairs from the provided Map.
	 * <p>This operation will overwrite any existing values. Use
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values.
	 */
	public void copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null || this.headers == headersToCopy) {
			return;
		}
		headersToCopy.forEach((key, value) -> {
			if (!isReadOnly(key)) {
				setHeader(key, value);
			}
		});
	}

	/**
	 * Copy the name-value pairs from the provided Map.
	 * <p>This operation will <em>not</em> overwrite any existing values.
	 */
	public void copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null || this.headers == headersToCopy) {
			return;
		}
		headersToCopy.forEach((key, value) -> {
			if (!isReadOnly(key)) {
				setHeaderIfAbsent(key, value);
			}
		});
	}

	protected boolean isReadOnly(String headerName) {
		return (MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName));
	}


	// Specific header accessors

	@Nullable
	public UUID getId() {
		Object value = getHeader(MessageHeaders.ID);
		if (value == null) {
			return null;
		}
		return (value instanceof UUID ? (UUID) value : UUID.fromString(value.toString()));
	}

	@Nullable
	public Long getTimestamp() {
		Object value = getHeader(MessageHeaders.TIMESTAMP);
		if (value == null) {
			return null;
		}
		return (value instanceof Long ? (Long) value : Long.parseLong(value.toString()));
	}

	public void setContentType(MimeType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	@Nullable
	public MimeType getContentType() {
		Object value = getHeader(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		return (value instanceof MimeType ? (MimeType) value : MimeType.valueOf(value.toString()));
	}

	private Charset getCharset() {
		MimeType contentType = getContentType();
		Charset charset = (contentType != null ? contentType.getCharset() : null);
		return (charset != null ? charset : DEFAULT_CHARSET);
	}

	public void setReplyChannelName(String replyChannelName) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	@Nullable
	public Object getReplyChannel() {
		return getHeader(MessageHeaders.REPLY_CHANNEL);
	}

	public void setErrorChannelName(String errorChannelName) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

	@Nullable
	public Object getErrorChannel() {
		return getHeader(MessageHeaders.ERROR_CHANNEL);
	}


	// Log message stuff

	/**
	 * Return a concise message for logging purposes.
	 * @param payload the payload that corresponds to the headers.
	 * @return the message
	 */
	public String getShortLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getShortPayloadLogMessage(payload);
	}

	/**
	 * Return a more detailed message for logging purposes.
	 * @param payload the payload that corresponds to the headers.
	 * @return the message
	 */
	public String getDetailedLogMessage(@Nullable Object payload) {
		return "headers=" + this.headers.toString() + getDetailedPayloadLogMessage(payload);
	}

	protected String getShortPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			String payloadText = (String) payload;
			return (payloadText.length() < 80) ?
				" payload=" + payloadText :
				" payload=" + payloadText.substring(0, 80) + "...(truncated)";
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				return (bytes.length < 80) ?
						" payload=" + new String(bytes, getCharset()) :
						" payload=" + new String(Arrays.copyOf(bytes, 80), getCharset()) + "...(truncated)";
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			String payloadText = payload.toString();
			return (payloadText.length() < 80) ?
					" payload=" + payloadText :
					" payload=" + ObjectUtils.identityToString(payload);
		}
	}

	protected String getDetailedPayloadLogMessage(@Nullable Object payload) {
		if (payload instanceof String) {
			return " payload=" + payload;
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				return " payload=" + new String(bytes, getCharset());
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			return " payload=" + payload;
		}
	}

	protected boolean isReadableContentType() {
		MimeType contentType = getContentType();
		for (MimeType mimeType : READABLE_MIME_TYPES) {
			if (mimeType.includes(contentType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [headers=" + this.headers + "]";
	}


	// Static factory methods

	/**
	 * Return the original {@code MessageHeaderAccessor} used to create the headers
	 * of the given {@code Message}, or {@code null} if that's not available or if
	 * its type does not match the required type.
	 * <p>This is for cases where the existence of an accessor is strongly expected
	 * (followed up with an assertion) or where an accessor will be created otherwise.
	 * @param message the message to get an accessor for
	 * @return an accessor instance of the specified type, or {@code null} if none
	 * @since 5.1.19
	 */
	@Nullable
	public static MessageHeaderAccessor getAccessor(Message<?> message) {
		return getAccessor(message.getHeaders(), null);
	}

	/**
	 * Return the original {@code MessageHeaderAccessor} used to create the headers
	 * of the given {@code Message}, or {@code null} if that's not available or if
	 * its type does not match the required type.
	 * <p>This is for cases where the existence of an accessor is strongly expected
	 * (followed up with an assertion) or where an accessor will be created otherwise.
	 * @param message the message to get an accessor for
	 * @param requiredType the required accessor type (or {@code null} for any)
	 * @return an accessor instance of the specified type, or {@code null} if none
	 * @since 4.1
	 */
	@Nullable
	public static <T extends MessageHeaderAccessor> T getAccessor(Message<?> message, @Nullable Class<T> requiredType) {
		return getAccessor(message.getHeaders(), requiredType);
	}

	/**
	 * A variation of {@link #getAccessor(org.springframework.messaging.Message, Class)}
	 * with a {@code MessageHeaders} instance instead of a {@code Message}.
	 * <p>This is for cases when a full message may not have been created yet.
	 * @param messageHeaders the message headers to get an accessor for
	 * @param requiredType the required accessor type (or {@code null} for any)
	 * @return an accessor instance of the specified type, or {@code null} if none
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends MessageHeaderAccessor> T getAccessor(
			MessageHeaders messageHeaders, @Nullable Class<T> requiredType) {

		if (messageHeaders instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) messageHeaders;
			MessageHeaderAccessor headerAccessor = mutableHeaders.getAccessor();
			if (requiredType == null || requiredType.isInstance(headerAccessor))  {
				return (T) headerAccessor;
			}
		}
		return null;
	}

	/**
	 * Return a mutable {@code MessageHeaderAccessor} for the given message attempting
	 * to match the type of accessor used to create the message headers, or otherwise
	 * wrapping the message with a {@code MessageHeaderAccessor} instance.
	 * <p>This is for cases where a header needs to be updated in generic code
	 * while preserving the accessor type for downstream processing.
	 * @return an accessor of the required type (never {@code null})
	 * @since 4.1
	 */
	public static MessageHeaderAccessor getMutableAccessor(Message<?> message) {
		if (message.getHeaders() instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) message.getHeaders();
			MessageHeaderAccessor accessor = mutableHeaders.getAccessor();
			return (accessor.isMutable() ? accessor : accessor.createAccessor(message));
		}
		return new MessageHeaderAccessor(message);
	}


	/**
	 * Extension of {@link MessageHeaders} that helps to preserve the link to
	 * the outer {@link MessageHeaderAccessor} instance that created it as well
	 * as keeps track of whether headers are still mutable.
	 */
	@SuppressWarnings("serial")
	private class MutableMessageHeaders extends MessageHeaders {

		private boolean mutable = true;

		public MutableMessageHeaders(@Nullable Map<String, Object> headers) {
			super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			Assert.state(this.mutable, "Already immutable");
			return super.getRawHeaders();
		}

		public void setImmutable() {
			if (!this.mutable) {
				return;
			}

			if (getId() == null) {
				IdGenerator idGenerator = (MessageHeaderAccessor.this.idGenerator != null ?
						MessageHeaderAccessor.this.idGenerator : MessageHeaders.getIdGenerator());
				UUID id = idGenerator.generateId();
				if (id != MessageHeaders.ID_VALUE_NONE) {
					getRawHeaders().put(ID, id);
				}
			}

			if (getTimestamp() == null) {
				if (MessageHeaderAccessor.this.enableTimestamp) {
					getRawHeaders().put(TIMESTAMP, System.currentTimeMillis());
				}
			}

			this.mutable = false;
		}

		public boolean isMutable() {
			return this.mutable;
		}

		public MessageHeaderAccessor getAccessor() {
			return MessageHeaderAccessor.this;
		}

		protected Object writeReplace() {
			// Serialize as regular MessageHeaders (without MessageHeaderAccessor reference)
			return new MessageHeaders(this);
		}
	}

}
