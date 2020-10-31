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

package org.springframework.messaging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.IdGenerator;

/**
 * The headers for a {@link Message}.
 *
 * <p><b>IMPORTANT</b>: This class is immutable. Any mutating operation such as
 * {@code put(..)}, {@code putAll(..)} and others will throw
 * {@link UnsupportedOperationException}.
 * <p>Subclasses do have access to the raw headers, however, via {@link #getRawHeaders()}.
 *
 * <p>One way to create message headers is to use the
 * {@link org.springframework.messaging.support.MessageBuilder MessageBuilder}:
 * <pre class="code">
 * MessageBuilder.withPayload("foo").setHeader("key1", "value1").setHeader("key2", "value2");
 * </pre>
 *
 * A second option is to create {@link org.springframework.messaging.support.GenericMessage}
 * passing a payload as {@link Object} and headers as a {@link Map java.util.Map}:
 * <pre class="code">
 * Map headers = new HashMap();
 * headers.put("key1", "value1");
 * headers.put("key2", "value2");
 * new GenericMessage("foo", headers);
 * </pre>
 *
 * A third option is to use {@link org.springframework.messaging.support.MessageHeaderAccessor}
 * or one of its subclasses to create specific categories of headers.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Gary Russell
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.messaging.support.MessageBuilder
 * @see org.springframework.messaging.support.MessageHeaderAccessor
 */
public class MessageHeaders implements Map<String, Object>, Serializable {

	/**
	 * UUID for none.
	 */
	public static final UUID ID_VALUE_NONE = new UUID(0,0);

	/**
	 * The key for the Message ID. This is an automatically generated UUID and
	 * should never be explicitly set in the header map <b>except</b> in the
	 * case of Message deserialization where the serialized Message's generated
	 * UUID is being restored.
	 */
	public static final String ID = "id";

	/**
	 * The key for the message timestamp.
	 */
	public static final String TIMESTAMP = "timestamp";

	/**
	 * The key for the message content type.
	 */
	public static final String CONTENT_TYPE = "contentType";

	/**
	 * The key for the message reply channel.
	 */
	public static final String REPLY_CHANNEL = "replyChannel";

	/**
	 * The key for the message error channel.
	 */
	public static final String ERROR_CHANNEL = "errorChannel";


	private static final long serialVersionUID = 7035068984263400920L;

	private static final Log logger = LogFactory.getLog(MessageHeaders.class);

	private static final IdGenerator defaultIdGenerator = new AlternativeJdkIdGenerator();

	@Nullable
	private static volatile IdGenerator idGenerator;


	private final Map<String, Object> headers;


	/**
	 * Construct a {@link MessageHeaders} with the given headers. An {@link #ID} and
	 * {@link #TIMESTAMP} headers will also be added, overriding any existing values.
	 * @param headers a map with headers to add
	 */
	public MessageHeaders(@Nullable Map<String, Object> headers) {
		this(headers, null, null);
	}

	/**
	 * Constructor providing control over the ID and TIMESTAMP header values.
	 * @param headers a map with headers to add
	 * @param id the {@link #ID} header value
	 * @param timestamp the {@link #TIMESTAMP} header value
	 */
	protected MessageHeaders(@Nullable Map<String, Object> headers, @Nullable UUID id, @Nullable Long timestamp) {
		this.headers = (headers != null ? new HashMap<>(headers) : new HashMap<>());

		if (id == null) {
			this.headers.put(ID, getIdGenerator().generateId());
		}
		else if (id == ID_VALUE_NONE) {
			this.headers.remove(ID);
		}
		else {
			this.headers.put(ID, id);
		}

		if (timestamp == null) {
			this.headers.put(TIMESTAMP, System.currentTimeMillis());
		}
		else if (timestamp < 0) {
			this.headers.remove(TIMESTAMP);
		}
		else {
			this.headers.put(TIMESTAMP, timestamp);
		}
	}

	/**
	 * Copy constructor which allows for ignoring certain entries.
	 * Used for serialization without non-serializable entries.
	 * @param original the MessageHeaders to copy
	 * @param keysToIgnore the keys of the entries to ignore
	 */
	private MessageHeaders(MessageHeaders original, Set<String> keysToIgnore) {
		this.headers = CollectionUtils.newHashMap(original.headers.size());
		original.headers.forEach((key, value) -> {
			if (!keysToIgnore.contains(key)) {
				this.headers.put(key, value);
			}
		});
	}


	protected Map<String, Object> getRawHeaders() {
		return this.headers;
	}

	protected static IdGenerator getIdGenerator() {
		IdGenerator generator = idGenerator;
		return (generator != null ? generator : defaultIdGenerator);
	}

	@Nullable
	public UUID getId() {
		return get(ID, UUID.class);
	}

	@Nullable
	public Long getTimestamp() {
		return get(TIMESTAMP, Long.class);
	}

	@Nullable
	public Object getReplyChannel() {
		return get(REPLY_CHANNEL);
	}

	@Nullable
	public Object getErrorChannel() {
		return get(ERROR_CHANNEL);
	}


	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(Object key, Class<T> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" +
					key + "'. Expected [" + type + "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}


	// Delegating Map implementation

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableMap(this.headers).entrySet();
	}

	@Override
	@Nullable
	public Object get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}


	// Unsupported Map operations

	/**
	 * Since MessageHeaders are immutable, the call to this method
	 * will result in {@link UnsupportedOperationException}.
	 */
	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method
	 * will result in {@link UnsupportedOperationException}.
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method
	 * will result in {@link UnsupportedOperationException}.
	 */
	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method
	 * will result in {@link UnsupportedOperationException}.
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}


	// Serialization methods

	private void writeObject(ObjectOutputStream out) throws IOException {
		Set<String> keysToIgnore = new HashSet<>();
		this.headers.forEach((key, value) -> {
			if (!(value instanceof Serializable)) {
				keysToIgnore.add(key);
			}
		});

		if (keysToIgnore.isEmpty()) {
			// All entries are serializable -> serialize the regular MessageHeaders instance
			out.defaultWriteObject();
		}
		else {
			// Some non-serializable entries -> serialize a temporary MessageHeaders copy
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring non-serializable message headers: " + keysToIgnore);
			}
			out.writeObject(new MessageHeaders(this, keysToIgnore));
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}


	// equals, hashCode, toString

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other ||
				(other instanceof MessageHeaders && this.headers.equals(((MessageHeaders) other).headers)));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}

}
