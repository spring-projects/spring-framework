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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;


/**
 * Can be used to prepare headers for a new STOMP message, or to access and/or modify
 * STOMP-specific headers of an existing message.
 * <p>
 * Use one of the static factory method in this class, then call getters and setters, and
 * at the end if necessary call {@link #toMap()} to obtain the updated headers
 * or call {@link #toNativeHeaderMap()} to obtain only the STOMP-specific headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderAccessor extends WebMessageHeaderAccesssor {

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

	public static final String LOGIN = "login";

	public static final String PASSCODE = "passcode";

	public static final String DESTINATION = "destination";

	public static final String CONTENT_TYPE = "content-type";

	public static final String CONTENT_LENGTH = "content-length";

	public static final String HEARTBEAT = "heart-beat";


	private static final AtomicLong messageIdCounter = new AtomicLong();


	/**
	 * A constructor for creating new STOMP message headers.
	 */
	private StompHeaderAccessor(StompCommand command, Map<String, List<String>> externalSourceHeaders) {
		super(command.getMessageType(), command, externalSourceHeaders);
		initWebMessageHeaders();
	}

	private void initWebMessageHeaders() {
		String destination = getFirstNativeHeader(DESTINATION);
		if (destination != null) {
			super.setDestination(destination);
		}
		String contentType = getFirstNativeHeader(CONTENT_TYPE);
		if (contentType != null) {
			super.setContentType(MediaType.parseMediaType(contentType));
		}
		if (StompCommand.SUBSCRIBE.equals(getStompCommand()) || StompCommand.UNSUBSCRIBE.equals(getStompCommand())) {
			if (getFirstNativeHeader(STOMP_ID) != null) {
				super.setSubscriptionId(getFirstNativeHeader(STOMP_ID));
			}
		}
	}

	/**
	 * A constructor for accessing and modifying existing message headers.
	 */
	private StompHeaderAccessor(Message<?> message) {
		super(message);
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
	 * Return STOMP headers including original, wrapped STOMP headers (if any) plus
	 * additional header updates made through accessor methods.
	 */
	@Override
	public Map<String, List<String>> toNativeHeaderMap() {

		Map<String, List<String>> result = super.toNativeHeaderMap();

		String destination = super.getDestination();
		if (destination != null) {
			result.put(DESTINATION, Arrays.asList(destination));
		}

		MediaType contentType = getContentType();
		if (contentType != null) {
			result.put(CONTENT_TYPE, Arrays.asList(contentType.toString()));
		}

		if (StompCommand.MESSAGE.equals(getStompCommand())) {
			String subscriptionId = getSubscriptionId();
			if (subscriptionId != null) {
				result.put(SUBSCRIPTION, Arrays.asList(subscriptionId));
			}
			else {
				logger.warn("STOMP MESSAGE frame should have a subscription: " + this.toString());
			}
			if ((getMessageId() == null)) {
				String messageId = getSessionId() + "-" + messageIdCounter.getAndIncrement();
				result.put(MESSAGE_ID, Arrays.asList(messageId));
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
		String rawValue = getFirstNativeHeader(ACCEPT_VERSION);
		return (rawValue != null) ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.<String>emptySet();
	}

	public void setAcceptVersion(String acceptVersion) {
		setNativeHeader(ACCEPT_VERSION, acceptVersion);
	}

	public void setHost(String host) {
		setNativeHeader(HOST, host);
	}

	public String getHost() {
		return getFirstNativeHeader(HOST);
	}

	@Override
	public void setDestination(String destination) {
		super.setDestination(destination);
		setNativeHeader(DESTINATION, destination);
	}

	@Override
	public void setDestinations(List<String> destinations) {
		Assert.isTrue((destinations != null) && (destinations.size() == 1), "STOMP allows one destination per message");
		super.setDestinations(destinations);
		setNativeHeader(DESTINATION, destinations.get(0));
	}

	public long[] getHeartbeat() {
		String rawValue = getFirstNativeHeader(HEARTBEAT);
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
			setNativeHeader(CONTENT_TYPE, mediaType.toString());
		}
	}

	public MediaType getContentType() {
		String value = getFirstNativeHeader(CONTENT_TYPE);
		return (value != null) ? MediaType.parseMediaType(value) : null;
	}

	public Integer getContentLength() {
		String contentLength = getFirstNativeHeader(CONTENT_LENGTH);
		return StringUtils.hasText(contentLength) ? new Integer(contentLength) : null;
	}

	public void setContentLength(int contentLength) {
		setNativeHeader(CONTENT_LENGTH, String.valueOf(contentLength));
	}

	public void setHeartbeat(long cx, long cy) {
		setNativeHeader(HEARTBEAT, StringUtils.arrayToCommaDelimitedString(new Object[] {cx, cy}));
	}

	public void setAck(String ack) {
		setNativeHeader(ACK, ack);
	}

	public String getAck() {
		return getFirstNativeHeader(ACK);
	}

	public void setNack(String nack) {
		setNativeHeader(NACK, nack);
	}

	public String getNack() {
		return getFirstNativeHeader(NACK);
	}

	public void setLogin(String login) {
		setNativeHeader(LOGIN, login);
	}

	public String getLogin() {
		return getFirstNativeHeader(LOGIN);
	}


	public void setPasscode(String passcode) {
		setNativeHeader(PASSCODE, passcode);
	}

	public String getPasscode() {
		return getFirstNativeHeader(PASSCODE);
	}

	public void setReceiptId(String receiptId) {
		setNativeHeader(RECEIPT_ID, receiptId);
	}

	public String getReceiptId() {
		return getFirstNativeHeader(RECEIPT_ID);
	}

	public String getMessage() {
		return getFirstNativeHeader(MESSAGE);
	}

	public void setMessage(String content) {
		setNativeHeader(MESSAGE, content);
	}

	public String getMessageId() {
		return getFirstNativeHeader(MESSAGE_ID);
	}

	public void setMessageId(String id) {
		setNativeHeader(MESSAGE_ID, id);
	}

	public String getVersion() {
		return getFirstNativeHeader(VERSION);
	}

	public void setVersion(String version) {
		setNativeHeader(VERSION, version);
	}

}
