/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Represents STOMP frame headers.
 *
 * <p>In addition to the normal methods defined by {@link Map}, this class offers
 * the following convenience methods:
 * <ul>
 * <li>{@link #getFirst(String)} return the first value for a header name</li>
 * <li>{@link #add(String, String)} add to the list of values for a header name</li>
 * <li>{@link #set(String, String)} set a header name to a single string value</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#Frames_and_Headers">
 * https://stomp.github.io/stomp-specification-1.2.html#Frames_and_Headers</a>
 */
public class StompHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = 7514642206528452544L;


	// Standard headers (as defined in the spec)

	public static final String CONTENT_TYPE = "content-type"; // SEND, MESSAGE, ERROR

	public static final String CONTENT_LENGTH = "content-length"; // SEND, MESSAGE, ERROR

	public static final String RECEIPT = "receipt"; // any client frame other than CONNECT

	// CONNECT

	public static final String HOST = "host";

	public static final String ACCEPT_VERSION = "accept-version";

	public static final String LOGIN = "login";

	public static final String PASSCODE = "passcode";

	public static final String HEARTBEAT = "heart-beat";

	// CONNECTED

	public static final String SESSION = "session";

	public static final String SERVER = "server";

	// SEND

	public static final String DESTINATION = "destination";

	// SUBSCRIBE, UNSUBSCRIBE

	public static final String ID = "id";

	public static final String ACK = "ack";

	// MESSAGE

	public static final String SUBSCRIPTION = "subscription";

	public static final String MESSAGE_ID = "message-id";

	// RECEIPT

	public static final String RECEIPT_ID = "receipt-id";


	private final Map<String, List<String>> headers;


	/**
	 * Create a new instance to be populated with new header values.
	 */
	public StompHeaders() {
		this(new LinkedMultiValueMap<>(3), false);
	}

	private StompHeaders(Map<String, List<String>> headers, boolean readOnly) {
		Assert.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map = new LinkedMultiValueMap<>(headers.size());
			headers.forEach((key, value) -> map.put(key, Collections.unmodifiableList(value)));
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
	}


	/**
	 * Set the content-type header.
	 * Applies to the SEND, MESSAGE, and ERROR frames.
	 */
	public void setContentType(@Nullable MimeType mimeType) {
		if (mimeType != null) {
			Assert.isTrue(!mimeType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
			Assert.isTrue(!mimeType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
			set(CONTENT_TYPE, mimeType.toString());
		}
		else {
			set(CONTENT_TYPE, null);
		}
	}

	/**
	 * Return the content-type header value.
	 */
	@Nullable
	public MimeType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MimeTypeUtils.parseMimeType(value) : null);
	}

	/**
	 * Set the content-length header.
	 * Applies to the SEND, MESSAGE, and ERROR frames.
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * Return the content-length header or -1 if unknown.
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * Set the receipt header.
	 * Applies to any client frame other than CONNECT.
	 */
	public void setReceipt(@Nullable String receipt) {
		set(RECEIPT, receipt);
	}

	/**
	 * Get the receipt header.
	 */
	@Nullable
	public String getReceipt() {
		return getFirst(RECEIPT);
	}

	/**
	 * Set the host header.
	 * Applies to the CONNECT frame.
	 */
	public void setHost(@Nullable String host) {
		set(HOST, host);
	}

	/**
	 * Get the host header.
	 */
	@Nullable
	public String getHost() {
		return getFirst(HOST);
	}

	/**
	 * Set the accept-version header. Must be one of "1.1", "1.2", or both.
	 * Applies to the CONNECT frame.
	 * @since 5.0.7
	 */
	public void setAcceptVersion(@Nullable String... acceptVersions) {
		if (ObjectUtils.isEmpty(acceptVersions)) {
			set(ACCEPT_VERSION, null);
			return;
		}
		Arrays.stream(acceptVersions).forEach(version ->
				Assert.isTrue(version != null && (version.equals("1.1") || version.equals("1.2")),
						() -> "Invalid version: " + version));
		set(ACCEPT_VERSION, StringUtils.arrayToCommaDelimitedString(acceptVersions));
	}

	/**
	 * Get the accept-version header.
	 * @since 5.0.7
	 */
	@Nullable
	public String[] getAcceptVersion() {
		String value = getFirst(ACCEPT_VERSION);
		return value != null ? StringUtils.commaDelimitedListToStringArray(value) : null;
	}

	/**
	 * Set the login header.
	 * Applies to the CONNECT frame.
	 */
	public void setLogin(@Nullable String login) {
		set(LOGIN, login);
	}

	/**
	 * Get the login header.
	 */
	@Nullable
	public String getLogin() {
		return getFirst(LOGIN);
	}

	/**
	 * Set the passcode header.
	 * Applies to the CONNECT frame.
	 */
	public void setPasscode(@Nullable String passcode) {
		set(PASSCODE, passcode);
	}

	/**
	 * Get the passcode header.
	 */
	@Nullable
	public String getPasscode() {
		return getFirst(PASSCODE);
	}

	/**
	 * Set the heartbeat header.
	 * Applies to the CONNECT and CONNECTED frames.
	 */
	public void setHeartbeat(@Nullable long[] heartbeat) {
		if (heartbeat == null || heartbeat.length != 2) {
			throw new IllegalArgumentException("Heart-beat array must be of length 2, not " +
					(heartbeat != null ? heartbeat.length : "null"));
		}
		String value = heartbeat[0] + "," + heartbeat[1];
		if (heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Heart-beat values cannot be negative: " + value);
		}
		set(HEARTBEAT, value);
	}

	/**
	 * Get the heartbeat header.
	 */
	@Nullable
	public long[] getHeartbeat() {
		String rawValue = getFirst(HEARTBEAT);
		String[] rawValues = StringUtils.split(rawValue, ",");
		if (rawValues == null) {
			return null;
		}
		return new long[] {Long.parseLong(rawValues[0]), Long.parseLong(rawValues[1])};
	}

	/**
	 * Whether heartbeats are enabled. Returns {@code false} if
	 * {@link #setHeartbeat} is set to "0,0", and {@code true} otherwise.
	 */
	public boolean isHeartbeatEnabled() {
		long[] heartbeat = getHeartbeat();
		return (heartbeat != null && heartbeat[0] != 0 && heartbeat[1] != 0);
	}

	/**
	 * Set the session header.
	 * Applies to the CONNECTED frame.
	 */
	public void setSession(@Nullable String session) {
		set(SESSION, session);
	}

	/**
	 * Get the session header.
	 */
	@Nullable
	public String getSession() {
		return getFirst(SESSION);
	}

	/**
	 * Set the server header.
	 * Applies to the CONNECTED frame.
	 */
	public void setServer(@Nullable String server) {
		set(SERVER, server);
	}

	/**
	 * Get the server header.
	 * Applies to the CONNECTED frame.
	 */
	@Nullable
	public String getServer() {
		return getFirst(SERVER);
	}

	/**
	 * Set the destination header.
	 */
	public void setDestination(@Nullable String destination) {
		set(DESTINATION, destination);
	}

	/**
	 * Get the destination header.
	 * Applies to the SEND, SUBSCRIBE, and MESSAGE frames.
	 */
	@Nullable
	public String getDestination() {
		return getFirst(DESTINATION);
	}

	/**
	 * Set the id header.
	 * Applies to the SUBSCR0BE, UNSUBSCRIBE, and ACK or NACK frames.
	 */
	public void setId(@Nullable String id) {
		set(ID, id);
	}

	/**
	 * Get the id header.
	 */
	@Nullable
	public String getId() {
		return getFirst(ID);
	}

	/**
	 * Set the ack header to one of "auto", "client", or "client-individual".
	 * Applies to the SUBSCRIBE and MESSAGE frames.
	 */
	public void setAck(@Nullable String ack) {
		set(ACK, ack);
	}

	/**
	 * Get the ack header.
	 */
	@Nullable
	public String getAck() {
		return getFirst(ACK);
	}

	/**
	 * Set the login header.
	 * Applies to the MESSAGE frame.
	 */
	public void setSubscription(@Nullable String subscription) {
		set(SUBSCRIPTION, subscription);
	}

	/**
	 * Get the subscription header.
	 */
	@Nullable
	public String getSubscription() {
		return getFirst(SUBSCRIPTION);
	}

	/**
	 * Set the message-id header.
	 * Applies to the MESSAGE frame.
	 */
	public void setMessageId(@Nullable String messageId) {
		set(MESSAGE_ID, messageId);
	}

	/**
	 * Get the message-id header.
	 */
	@Nullable
	public String getMessageId() {
		return getFirst(MESSAGE_ID);
	}

	/**
	 * Set the receipt-id header.
	 * Applies to the RECEIPT frame.
	 */
	public void setReceiptId(@Nullable String receiptId) {
		set(RECEIPT_ID, receiptId);
	}

	/**
	 * Get the receipt header.
	 */
	@Nullable
	public String getReceiptId() {
		return getFirst(RECEIPT_ID);
	}

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value, or {@code null} if none
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		List<String> headerValues = this.headers.get(headerName);
		return headerValues != null ? headerValues.get(0) : null;
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		List<String> headerValues = this.headers.computeIfAbsent(headerName, k -> new ArrayList<>(1));
		headerValues.add(headerValue);
	}

	@Override
	public void addAll(String headerName, List<? extends String> headerValues) {
		List<String> currentValues = this.headers.computeIfAbsent(headerName, k -> new ArrayList<>(1));
		currentValues.addAll(headerValues);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		List<String> headerValues = new ArrayList<>(1);
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
		this.headers.forEach((key, value) -> singleValueMap.put(key, value.get(0)));
		return singleValueMap;
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof StompHeaders &&
				this.headers.equals(((StompHeaders) other).headers)));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}


	/**
	 * Return a {@code StompHeaders} object that can only be read, not written to.
	 */
	public static StompHeaders readOnlyStompHeaders(@Nullable Map<String, List<String>> headers) {
		return new StompHeaders((headers != null ? headers : Collections.emptyMap()), true);
	}

}
