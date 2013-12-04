/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * An extension of {@link MessageHeaderAccessor} that also stores and provides read/write
 * access to message headers from an external source -- e.g. a Spring {@link Message}
 * created to represent a STOMP message received from a STOMP client or message broker.
 * Native message headers are kept in a {@link MultiValueMap} under the key
 * {@link #NATIVE_HEADERS}.
 * <p>
 * This class is not intended for direct use but is rather expected to be consumed
 * through sub-classes such as
 * {@link org.springframework.messaging.simp.stomp.StompHeaderAccessor StompHeaderAccessor}.
 * Such sub-classes may provide factory methods to translate message headers from
 * an external messaging source (e.g. STOMP) to Spring {@link Message} headers and
 * reversely to translate Spring {@link Message} headers to a message to send to an
 * external source.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class NativeMessageHeaderAccessor extends MessageHeaderAccessor {

	public static final String NATIVE_HEADERS = "nativeHeaders";


	// wrapped native headers
	private final Map<String, List<String>> originalNativeHeaders;

	// native header updates
	private final MultiValueMap<String, String> nativeHeaders = new LinkedMultiValueMap<String, String>(4);


	/**
	 * A constructor for creating new headers, accepting an optional native header map.
	 */
	protected NativeMessageHeaderAccessor(Map<String, List<String>> nativeHeaders) {
		this.originalNativeHeaders = nativeHeaders;
	}

	/**
	 * A constructor for accessing and modifying existing message headers.
	 */
	protected NativeMessageHeaderAccessor(Message<?> message) {
		super(message);
		this.originalNativeHeaders = initNativeHeaders(message);
	}

	private static Map<String, List<String>> initNativeHeaders(Message<?> message) {
		if (message != null) {
			@SuppressWarnings("unchecked")
			Map<String, List<String>> headers = (Map<String, List<String>>) message.getHeaders().get(NATIVE_HEADERS);
			if (headers != null) {
				return headers;
			}
		}
		return null;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> result = super.toMap();
		result.put(NATIVE_HEADERS, toNativeHeaderMap());
		return result;
	}

	@Override
	public boolean isModified() {
		return (super.isModified() || (!this.nativeHeaders.isEmpty()));
	}

	/**
	 * Return a map with native headers including original, wrapped headers (if any) plus
	 * additional header updates made through accessor methods.
	 */
	public Map<String, List<String>> toNativeHeaderMap() {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		if (this.originalNativeHeaders != null) {
			result.putAll(this.originalNativeHeaders);
		}
		for (String key : this.nativeHeaders.keySet()) {
			List<String> value = this.nativeHeaders.get(key);
			if (value == null) {
				result.remove(key);
			}
			else {
				result.put(key, value);
			}
		}
		return result;
	}

	/**
	 * Return all values for the specified native header or {@code null}.
	 */
	public List<String> getNativeHeader(String headerName) {
		if (this.nativeHeaders.containsKey(headerName)) {
			return this.nativeHeaders.get(headerName);
		}
		else if (this.originalNativeHeaders != null) {
			return this.originalNativeHeaders.get(headerName);
		}
		return null;
	}

	/**
	 * Return the first value for the specified native header of {@code null}.
	 */
	public String getFirstNativeHeader(String headerName) {
		List<String> values = getNativeHeader(headerName);
		return CollectionUtils.isEmpty(values) ? null : values.get(0);
	}

	/**
	 * Set the specified native header value.
	 */
	public void setNativeHeader(String name, String value) {
		if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
			this.nativeHeaders.set(name, value);
		}
	}

	/**
	 * Add the specified native header value.
	 */
	public void addNativeHeader(String name, String value) {
		this.nativeHeaders.add(name, value);
	}

}
