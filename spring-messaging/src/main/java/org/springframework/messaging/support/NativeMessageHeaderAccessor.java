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
	 * Protected constructor to create a new instance.
	 */
	protected NativeMessageHeaderAccessor() {
		this((Map<String, List<String>>) null);
	}

	/**
	 * Protected constructor to create an instance with the given native headers.
	 * @param nativeHeaders native headers to create the message with (may be {@code null})
	 */
	protected NativeMessageHeaderAccessor(@Nullable Map<String, List<String>> nativeHeaders) {
		if (!CollectionUtils.isEmpty(nativeHeaders)) {
			setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<>(nativeHeaders));
		}
	}

	/**
	 * Protected constructor that copies headers from another message.
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


	/**
	 * Subclasses can use this method to access the "native" headers sub-map.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected Map<String, List<String>> getNativeHeaders() {
		return (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
	}

	/**
	 * Return a copy of the native headers sub-map, or an empty map.
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
	 * @param headerName the name of the header
	 */
	public boolean containsNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null && map.containsKey(headerName));
	}

	/**
	 * Return all values for the specified native header, if present.
	 * @param headerName the name of the header
	 * @return the associated values, or {@code null} if none
	 */
	@Nullable
	public List<String> getNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? map.get(headerName) : null);
	}

	/**
	 * Return the first value for the specified native header, if present.
	 * @param headerName the name of the header
	 * @return the associated value, or {@code null} if none
	 */
	@Nullable
	public String getFirstNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		if (map != null) {
			List<String> values = map.get(headerName);
			if (!CollectionUtils.isEmpty(values)) {
				return values.get(0);
			}
		}
		return null;
	}

	/**
	 * Set the specified native header value replacing existing values.
	 * <p>In order for this to work, the accessor must be {@link #isMutable()
	 * mutable}. See {@link MessageHeaderAccessor} for details.
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
	 * <p>In order for this to work, the accessor must be {@link #isMutable()
	 * mutable}. See {@link MessageHeaderAccessor} for details.
	 * @param name the name of the header
	 * @param value the header value to set
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
		List<String> values = nativeHeaders.computeIfAbsent(name, k -> new LinkedList<>());
		values.add(value);
		setModified(true);
	}

	/**
	 * Add the specified native headers to existing values.
	 * @param headers the headers to set
	 */
	public void addNativeHeaders(@Nullable MultiValueMap<String, String> headers) {
		if (headers == null) {
			return;
		}
		headers.forEach((key, values) -> values.forEach(value -> addNativeHeader(key, value)));
	}

	/**
	 * Remove the specified native header value replacing existing values.
	 * <p>In order for this to work, the accessor must be {@link #isMutable()
	 * mutable}. See {@link MessageHeaderAccessor} for details.
	 * @param headerName the name of the header
	 * @return the associated values, or {@code null} if the header was not present
	 */
	@Nullable
	public List<String> removeNativeHeader(String headerName) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (CollectionUtils.isEmpty(nativeHeaders)) {
			return null;
		}
		return nativeHeaders.remove(headerName);
	}


	/**
	 * Return the first value for the specified native header,
	 * or {@code null} if none.
	 * @param headerName the name of the header
	 * @param headers the headers map to introspect
	 * @return the associated value, or {@code null} if none
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static String getFirstNativeHeader(String headerName, Map<String, Object> headers) {
		Map<String, List<String>> map = (Map<String, List<String>>) headers.get(NATIVE_HEADERS);
		if (map != null) {
			List<String> values = map.get(headerName);
			if (!CollectionUtils.isEmpty(values)) {
				return values.get(0);
			}
		}
		return null;
	}

}
