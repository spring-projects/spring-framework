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

package org.springframework.messaging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

/**
 * The headers for a {@link Message}
 * <p>
 * <b>IMPORTANT</b>: This class is immutable. Any mutating operation such as
 * {@code put(..)}, {@code putAll(..)} and others will throw
 * {@link UnsupportedOperationException}.
 * <p>
 * One way to create message headers is to use the
 * {@link org.springframework.messaging.support.MessageBuilder MessageBuilder}:
 * <pre class="code">
 * MessageBuilder.withPayload("foo").setHeader("key1", "value1").setHeader("key2", "value2");
 * </pre>
 * A second option is to create {@link org.springframework.messaging.support.GenericMessage}
 * passing a payload as {@link Object} and headers as a {@link Map java.util.Map}:
 * <pre class="code">
 * Map headers = new HashMap();
 * headers.put("key1", "value1");
 * headers.put("key2", "value2");
 * new GenericMessage("foo", headers);
 * </pre>
 * A third option is to use {@link org.springframework.messaging.support.MessageHeaderAccessor}
 * or one of its sub-classes to create specific categories of headers.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Gary Russell
 * @since 4.0
 * @see org.springframework.messaging.support.MessageBuilder
 * @see org.springframework.messaging.support.MessageHeaderAccessor
 */
public final class MessageHeaders implements Map<String, Object>, Serializable {

	private static final long serialVersionUID = -4615750558355702881L;

	private static final Log logger = LogFactory.getLog(MessageHeaders.class);

	private static volatile IdGenerator idGenerator = null;

	private static final IdGenerator defaultIdGenerator = new AlternativeJdkIdGenerator();

	/**
	 * The key for the Message ID. This is an automatically generated UUID and
	 * should never be explicitly set in the header map <b>except</b> in the
	 * case of Message deserialization where the serialized Message's generated
	 * UUID is being restored.
	 */
	public static final String ID = "id";

	public static final String TIMESTAMP = "timestamp";

	public static final String REPLY_CHANNEL = "replyChannel";

	public static final String ERROR_CHANNEL = "errorChannel";

	public static final String CONTENT_TYPE = "contentType";


	private final Map<String, Object> headers;


	public MessageHeaders(Map<String, Object> headers) {
		this.headers = (headers != null) ? new HashMap<String, Object>(headers) : new HashMap<String, Object>();
		this.headers.put(ID, ((idGenerator != null) ? idGenerator : defaultIdGenerator).generateId());
		this.headers.put(TIMESTAMP, System.currentTimeMillis());
	}


	public UUID getId() {
		return this.get(ID, UUID.class);
	}

	public Long getTimestamp() {
		return this.get(TIMESTAMP, Long.class);
	}

	public Object getReplyChannel() {
		return this.get(REPLY_CHANNEL);
	}

	public Object getErrorChannel() {
		return this.get(ERROR_CHANNEL);
	}

	@SuppressWarnings("unchecked")
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

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object != null && object instanceof MessageHeaders) {
			MessageHeaders other = (MessageHeaders) object;
			return this.headers.equals(other.headers);
		}
		return false;
	}

	@Override
	public String toString() {
		Map<String, Object> map = new LinkedHashMap<String, Object>(this.headers);
		map.put(ID,  map.remove(ID)); // remove and add again at the end
		map.put(TIMESTAMP, map.remove(TIMESTAMP));
		return map.toString();
	}

	/*
	 * Map implementation
	 */

	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(this.headers.entrySet());
	}

	public Object get(Object key) {
		return this.headers.get(key);
	}

	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	public int size() {
		return this.headers.size();
	}

	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}

	// Unsupported operations

	/**
	 * Since MessageHeaders are immutable, the call to this method will result in {@link UnsupportedOperationException}.
	 */
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method will result in {@link UnsupportedOperationException}.
	 */
	public void putAll(Map<? extends String, ? extends Object> t) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method will result in {@link UnsupportedOperationException}.
	 */
	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * Since MessageHeaders are immutable, the call to this method will result in {@link UnsupportedOperationException}.
	 */
	public void clear() {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	// Serialization methods

	private void writeObject(ObjectOutputStream out) throws IOException {
		List<String> keysToRemove = new ArrayList<String>();
		for (Map.Entry<String, Object> entry : this.headers.entrySet()) {
			if (!(entry.getValue() instanceof Serializable)) {
				keysToRemove.add(entry.getKey());
			}
		}
		for (String key : keysToRemove) {
			if (logger.isInfoEnabled()) {
				logger.info("removing non-serializable header: " + key);
			}
			this.headers.remove(key);
		}
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

}
