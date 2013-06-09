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

package org.springframework.web.messaging.stomp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = 1L;

	// TODO: separate client from server headers so they can't be mixed

	// Client
	private static final String ID = "id";

	private static final String HOST = "host";

	private static final String ACCEPT_VERSION = "accept-version";

	// Server

	private static final String MESSAGE_ID = "message-id";

	private static final String RECEIPT_ID = "receipt-id";

	private static final String SUBSCRIPTION = "subscription";

	private static final String VERSION = "version";

	private static final String MESSAGE = "message";

	// Client and Server

	private static final String ACK = "ack";

	private static final String DESTINATION = "destination";

	private static final String CONTENT_TYPE = "content-type";

	private static final String CONTENT_LENGTH = "content-length";

	private static final String HEARTBEAT = "heart-beat";


	public static final List<String> STANDARD_HEADER_NAMES =
			Arrays.asList(ID, HOST, ACCEPT_VERSION, MESSAGE_ID, RECEIPT_ID, SUBSCRIPTION,
					VERSION, MESSAGE, ACK, DESTINATION, CONTENT_LENGTH, CONTENT_TYPE, HEARTBEAT);


	private final Map<String, List<String>> headers;


	/**
	 * Private constructor that can create read-only {@code StompHeaders} instances.
	 */
	private StompHeaders(Map<String, List<String>> headers, boolean readOnly) {
		Assert.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map = new LinkedHashMap<String, List<String>>(headers.size());
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				List<String> values = Collections.unmodifiableList(entry.getValue());
				map.put(entry.getKey(), values);
			}
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
	}

	/**
	 * Constructs a new, empty instance of the {@code StompHeaders} object.
	 */
	public StompHeaders() {
		this(new LinkedHashMap<String, List<String>>(4), false);
	}

	/**
	 * Returns {@code StompHeaders} object that can only be read, not written to.
	 */
	public static StompHeaders readOnlyStompHeaders(StompHeaders headers) {
		return new StompHeaders(headers, true);
	}

	public Set<String> getAcceptVersion() {
		String rawValue = getFirst(ACCEPT_VERSION);
		return (rawValue != null) ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.<String>emptySet();
	}

	public void setAcceptVersion(String acceptVersion) {
		set(ACCEPT_VERSION, acceptVersion);
	}

	public String getVersion() {
		return getFirst(VERSION);
	}

	public void setVersion(String version) {
		set(VERSION, version);
	}

	public String getDestination() {
		return getFirst(DESTINATION);
	}

	public void setDestination(String destination) {
		set(DESTINATION, destination);
	}

	public MediaType getContentType() {
		String contentType = getFirst(CONTENT_TYPE);
		return StringUtils.hasText(contentType) ? MediaType.valueOf(contentType) : null;
	}

	public void setContentType(MediaType mediaType) {
		if (mediaType != null) {
			set(CONTENT_TYPE, mediaType.toString());
		}
		else {
			remove(CONTENT_TYPE);
		}
	}

	public Integer getContentLength() {
		String contentLength = getFirst(CONTENT_LENGTH);
		return StringUtils.hasText(contentLength) ? new Integer(contentLength) : null;
	}

	public void setContentLength(int contentLength) {
		set(CONTENT_LENGTH, String.valueOf(contentLength));
	}

	public long[] getHeartbeat() {
		String rawValue = getFirst(HEARTBEAT);
		if (!StringUtils.hasText(rawValue)) {
			return null;
		}
		String[] rawValues = StringUtils.commaDelimitedListToStringArray(rawValue);
		// TODO assertions
		return new long[] { Long.valueOf(rawValues[0]), Long.valueOf(rawValues[1])};
	}

	public void setHeartbeat(long cx, long cy) {
		set(HEARTBEAT, StringUtils.arrayToCommaDelimitedString(new Object[] {cx, cy}));
	}

	public String getId() {
		return getFirst(ID);
	}

	public void setId(String id) {
		set(ID, id);
	}

	public String getMessageId() {
		return getFirst(MESSAGE_ID);
	}

	public void setMessageId(String id) {
		set(MESSAGE_ID, id);
	}

	public String getSubscription() {
		return getFirst(SUBSCRIPTION);
	}

	public void setSubscription(String id) {
		set(SUBSCRIPTION, id);
	}

	public String getMessage() {
		return getFirst(MESSAGE);
	}

	public void setMessage(String id) {
		set(MESSAGE, id);
	}


	// MultiValueMap methods

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value; or {@code null}
	 */
	public String getFirst(String headerName) {
		List<String> headerValues = headers.get(headerName);
		return headerValues != null ? headerValues.get(0) : null;
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	public void add(String headerName, String headerValue) {
		List<String> headerValues = headers.get(headerName);
		if (headerValues == null) {
			headerValues = new LinkedList<String>();
			this.headers.put(headerName, headerValues);
		}
		headerValues.add(headerValue);
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	public void set(String headerName, String headerValue) {
		List<String> headerValues = new LinkedList<String>();
		headerValues.add(headerValue);
		headers.put(headerName, headerValues);
	}

	public void setAll(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = new LinkedHashMap<String,String>(this.headers.size());
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			singleValueMap.put(entry.getKey(), entry.getValue().get(0));
		}
		return singleValueMap;
	}


	// Map implementation

	public int size() {
		return this.headers.size();
	}

	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	public void putAll(Map<? extends String, ? extends List<String>> m) {
		this.headers.putAll(m);
	}

	public void clear() {
		this.headers.clear();
	}

	public Set<String> keySet() {
		return this.headers.keySet();
	}

	public Collection<List<String>> values() {
		return this.headers.values();
	}

	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof StompHeaders)) {
			return false;
		}
		StompHeaders otherHeaders = (StompHeaders) other;
		return this.headers.equals(otherHeaders.headers);
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
