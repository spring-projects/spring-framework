/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * An extension of {@link MessageHeaderAccessor} that also stores and provides read/write
 * access to message headers from an external source -- e.g. a Spring {@link Message}
 * created to represent a STOMP message received from a STOMP client or message broker.
 * Native message headers are kept in a {@code Map<String, List<String>>} under the key
 * {@link #NATIVE_HEADERS}.
 *
 * <p>This class is not intended for direct use but is rather expected to be used
 * indirectly through protocol-specific sub-classes such as
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


	/**
	 * A protected constructor to create new headers.
	 */
	protected NativeMessageHeaderAccessor() {
		this((Map<String, List<String>>) null);
	}

	/**
	 * A protected constructor to create new headers.
	 * @param nativeHeaders native headers to create the message with (may be {@code null})
	 */
	protected NativeMessageHeaderAccessor(@Nullable Map<String, List<String>> nativeHeaders) {
		if (!CollectionUtils.isEmpty(nativeHeaders)) {
			setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<>(nativeHeaders));
		}
	}

	/**
	 * A protected constructor accepting the headers of an existing message to copy.
	 */
	protected NativeMessageHeaderAccessor(@Nullable Message<?> message) {
		super(message);
		if (message != null) {
			@SuppressWarnings("unchecked")
			Map<String, List<String>> map = (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
			if (map != null) {
				// Force removal since setHeader checks for equality
				removeHeader(NATIVE_HEADERS);
				setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<>(map));
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected Map<String, List<String>> getNativeHeaders() {
		return (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
	}

	/**
	 * Return a copy of the native header values or an empty map.
	 */
	public Map<String, List<String>> toNativeHeaderMap() {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? new LinkedMultiValueMap<>(map) : Collections.emptyMap());
	}

	@Override
	public void setImmutable() {
		if (isMutable()) {
			Map<String, List<String>> map = getNativeHeaders();
			if (map != null) {
				// Force removal since setHeader checks for equality
				removeHeader(NATIVE_HEADERS);
				setHeader(NATIVE_HEADERS, Collections.unmodifiableMap(map));
			}
			super.setImmutable();
		}
	}

	/**
	 * Whether the native header map contains the give header name.
	 */
	public boolean containsNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null && map.containsKey(headerName));
	}

	/**
	 * Return all values for the specified native header.
	 * or {@code null} if none.
	 */
	@Nullable
	public List<String> getNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? map.get(headerName) : null);
	}

	/**
	 * Return the first value for the specified native header,
	 * or {@code null} if none.
	 */
	@Nullable
	public String getFirstNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		if (map != null) {
			List<String> values = map.get(headerName);
			if (values != null) {
				return values.get(0);
			}
		}
		return null;
	}

	/**
	 * Set the specified native header value replacing existing values.
	 */
	public void setNativeHeader(String name, @Nullable String value) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> map = getNativeHeaders();
		if (value == null) {
			if (map != null && map.get(name) != null) {
				setModified(true);
				map.remove(name);
			}
			return;
		}
		if (map == null) {
			map = new LinkedMultiValueMap<>(4);
			setHeader(NATIVE_HEADERS, map);
		}
		List<String> values = new LinkedList<>();
		values.add(value);
		if (!ObjectUtils.nullSafeEquals(values, getHeader(name))) {
			setModified(true);
			map.put(name, values);
		}
	}

	/**
	 * Add the specified native header value to existing values.
	 */
	public void addNativeHeader(String name, @Nullable String value) {
		Assert.state(isMutable(), "Already immutable");
		if (value == null) {
			return;
		}
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders == null) {
			nativeHeaders = new LinkedMultiValueMap<>(4);
			setHeader(NATIVE_HEADERS, nativeHeaders);
		}
		List<String> values = nativeHeaders.get(name);
		if (values == null) {
			values = new LinkedList<>();
			nativeHeaders.put(name, values);
		}
		values.add(value);
		setModified(true);
	}

	public void addNativeHeaders(@Nullable MultiValueMap<String, String> headers) {
		if (headers == null) {
			return;
		}
		headers.forEach((key, values) ->
				values.forEach(value -> addNativeHeader(key, value)));
	}

	@Nullable
	public List<String> removeNativeHeader(String name) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders == null) {
			return null;
		}
		return nativeHeaders.remove(name);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static String getFirstNativeHeader(String headerName, Map<String, Object> headers) {
		Map<String, List<String>> map = (Map<String, List<String>>) headers.get(NATIVE_HEADERS);
		if (map != null) {
			List<String> values = map.get(headerName);
			if (values != null) {
				return values.get(0);
			}
		}
		return null;
	}

}
