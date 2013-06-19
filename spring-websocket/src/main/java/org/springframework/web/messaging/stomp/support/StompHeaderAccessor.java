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

package org.springframework.web.messaging.stomp.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.support.PubSubHeaderAccesssor;


/**
 * Can be used to prepare headers for a new STOMP message, or to access and/or modify
 * STOMP-specific headers of an existing message.
 * <p>
 * Use one of the static factory method in this class, then call getters and setters, and
 * at the end if necessary call {@link #toHeaders()} to obtain the updated headers
 * or call {@link #toStompMessageHeaders()} to obtain only the STOMP-specific headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderAccessor extends PubSubHeaderAccesssor {

	public static final String STOMP_ID = "id";

	public static final String HOST = "host";

	public static final String ACCEPT_VERSION = "accept-version";

	public static final String MESSAGE_ID = "message-id";

	public static final String RECEIPT_ID = "receipt-id";

	public static final String SUBSCRIPTION = "subscription";

	public static final String VERSION = "version";

	public static final String MESSAGE = "message";

	public static final String ACK = "ack";

	public static final String NACK = "nack";

	public static final String DESTINATION = "destination";

	public static final String CONTENT_TYPE = "content-type";

	public static final String CONTENT_LENGTH = "content-length";

	public static final String HEARTBEAT = "heart-beat";


	private static final String STOMP_HEADERS = "stompHeaders";

	private static final AtomicLong messageIdCounter = new AtomicLong();


	private final Map<String, String> headers;


	/**
	 * A constructor for creating new STOMP message headers.
	 * This constructor is private. See factory methods in this sub-classes.
	 */
	private StompHeaderAccessor(StompCommand command, Map<String, List<String>> externalSourceHeaders) {
		super(command.getMessageType(), command, externalSourceHeaders);
		this.headers = new HashMap<String, String>(4);
		updateMessageHeaders();
	}

	private void updateMessageHeaders() {
		if (getExternalSourceHeaders().isEmpty()) {
			return;
		}
		String destination = getHeaderValue(DESTINATION);
		if (destination != null) {
			super.setDestination(destination);
		}
		String contentType = getHeaderValue(CONTENT_TYPE);
		if (contentType != null) {
			super.setContentType(MediaType.parseMediaType(contentType));
		}
		if (StompCommand.SUBSCRIBE.equals(getStompCommand())) {
			if (getHeaderValue(STOMP_ID) != null) {
				super.setSubscriptionId(getHeaderValue(STOMP_ID));
			}
		}
	}

	/**
	 * A constructor for accessing and modifying existing message headers. This
	 * constructor is protected. See factory methods in this class.
	 */
	@SuppressWarnings("unchecked")
	private StompHeaderAccessor(Message<?> message) {
		super(message);
		this.headers = (message.getHeaders() .get(STOMP_HEADERS) != null) ?
				(Map<String, String>) message.getHeaders().get(STOMP_HEADERS) : new HashMap<String, String>(4);
	}


	/**
	 * Create {@link StompHeaderAccessor} for a new {@link Message}.
	 */
	public static StompHeaderAccessor create(StompCommand command) {
		return new StompHeaderAccessor(command, null);
	}

	/**
	 * Create {@link StompHeaderAccessor} from parsed STOP frame content.
	 */
	public static StompHeaderAccessor create(StompCommand command, Map<String, List<String>> headers) {
		return new StompHeaderAccessor(command, headers);
	}

	/**
	 * Create {@link StompHeaderAccessor} from the headers of an existing {@link Message}.
	 */
	public static StompHeaderAccessor wrap(Message<?> message) {
		return new StompHeaderAccessor(message);
	}


	/**
	 * Return the original, wrapped headers (i.e. unmodified) or a new Map including any
	 * updates made via setters.
	 */
	@Override
	public Map<String, Object> toHeaders() {
		Map<String, Object> result = super.toHeaders();
		if (isModified()) {
			result.put(STOMP_HEADERS, this.headers);
		}
		return result;
	}

	@Override
	public boolean isModified() {
		return (super.isModified() || !this.headers.isEmpty());
	}

	/**
	 * Return STOMP headers and any custom headers that may have been sent by
	 * a remote endpoint, if this message originated from outside.
	 */
	public Map<String, List<String>> toStompMessageHeaders() {

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
		result.putAll(getExternalSourceHeaders());
		result.setAll(this.headers);

		String destination = super.getDestination();
		if (destination != null) {
			result.set(DESTINATION, destination);
		}

		MediaType contentType = getContentType();
		if (contentType != null) {
			result.set(CONTENT_TYPE, contentType.toString());
		}

		if (StompCommand.MESSAGE.equals(getStompCommand())) {
			String subscriptionId = getSubscriptionId();
			if (subscriptionId != null) {
				result.set(SUBSCRIPTION, subscriptionId);
			}
			else {
				logger.warn("STOMP MESSAGE frame should have a subscription: " + this.toString());
			}
			if ((getMessageId() == null)) {
				result.set(MESSAGE_ID, getSessionId() + "-" + messageIdCounter.getAndIncrement());
			}
		}

		return result;
	}

	public void setStompCommandIfNotSet(StompCommand command) {
		if (getStompCommand() == null) {
			setProtocolMessageType(command);
		}
	}

	public StompCommand getStompCommand() {
		return (StompCommand) super.getProtocolMessageType();
	}

	public Set<String> getAcceptVersion() {
		String rawValue = getHeaderValue(ACCEPT_VERSION);
		return (rawValue != null) ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.<String>emptySet();
	}

	private String getHeaderValue(String headerName) {
		List<String> values = getExternalSourceHeaders().get(headerName);
		return !CollectionUtils.isEmpty(values) ? values.get(0) : this.headers.get(headerName);
	}

	public void setAcceptVersion(String acceptVersion) {
		this.headers.put(ACCEPT_VERSION, acceptVersion);
	}

	public void setHost(String host) {
		this.headers.put(HOST, host);
	}

	public String getHost() {
		return getHeaderValue(HOST);
	}

	@Override
	public void setDestination(String destination) {
		super.setDestination(destination);
		this.headers.put(DESTINATION, destination);
	}

	@Override
	public void setDestinations(List<String> destinations) {
		Assert.isTrue((destinations != null) && (destinations.size() == 1), "STOMP allows one destination per message");
		super.setDestinations(destinations);
		this.headers.put(DESTINATION, destinations.get(0));
	}

	public long[] getHeartbeat() {
		String rawValue = getHeaderValue(HEARTBEAT);
		if (!StringUtils.hasText(rawValue)) {
			return null;
		}
		String[] rawValues = StringUtils.commaDelimitedListToStringArray(rawValue);
		// TODO assertions
		return new long[] { Long.valueOf(rawValues[0]), Long.valueOf(rawValues[1])};
	}

	public void setContentType(MediaType mediaType) {
		if (mediaType != null) {
			super.setContentType(mediaType);
			this.headers.put(CONTENT_TYPE, mediaType.toString());
		}
	}

	public MediaType getContentType() {
		String value = getHeaderValue(CONTENT_TYPE);
		return (value != null) ? MediaType.parseMediaType(value) : null;
	}

	public Integer getContentLength() {
		String contentLength = getHeaderValue(CONTENT_LENGTH);
		return StringUtils.hasText(contentLength) ? new Integer(contentLength) : null;
	}

	public void setContentLength(int contentLength) {
		this.headers.put(CONTENT_LENGTH, String.valueOf(contentLength));
	}

	public void setHeartbeat(long cx, long cy) {
		this.headers.put(HEARTBEAT, StringUtils.arrayToCommaDelimitedString(new Object[] {cx, cy}));
	}

	public void setAck(String ack) {
		this.headers.put(ACK, ack);
	}

	public String getAck() {
		return getHeaderValue(ACK);
	}

	public void setNack(String nack) {
		this.headers.put(NACK, nack);
	}

	public String getNack() {
		return getHeaderValue(NACK);
	}

	public void setReceiptId(String receiptId) {
		this.headers.put(RECEIPT_ID, receiptId);
	}

	public String getReceiptId() {
		return getHeaderValue(RECEIPT_ID);
	}

	public String getMessage() {
		return getHeaderValue(MESSAGE);
	}

	public void setMessage(String content) {
		this.headers.put(MESSAGE, content);
	}

	public String getMessageId() {
		return getHeaderValue(MESSAGE_ID);
	}

	public void setMessageId(String id) {
		this.headers.put(MESSAGE_ID, id);
	}

	public String getVersion() {
		return getHeaderValue(VERSION);
	}

	public void setVersion(String version) {
		this.headers.put(VERSION, version);
	}

	@Override
	public String toString() {
		return "StompHeaders [" + "messageType=" + getMessageType() + ", protocolMessageType="
				+ getProtocolMessageType() + ", destination=" + getDestination()
				+ ", subscriptionId=" + getSubscriptionId() + ", sessionId=" + getSessionId()
				+ ", externalSourceHeaders=" + getExternalSourceHeaders() + ", headers=" + this.headers + "]";
	}

}
