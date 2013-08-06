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

package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


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
public class StompHeaderAccessor extends SimpMessageHeaderAccessor {

	// STOMP header names

	public static final String STOMP_ID_HEADER = "id";

	public static final String STOMP_HOST_HEADER = "host";

	public static final String STOMP_ACCEPT_VERSION_HEADER = "accept-version";

	public static final String STOMP_MESSAGE_ID_HEADER = "message-id";

	public static final String STOMP_RECEIPT_ID_HEADER = "receipt-id";

	public static final String STOMP_SUBSCRIPTION_HEADER = "subscription";

	public static final String STOMP_VERSION_HEADER = "version";

	public static final String STOMP_MESSAGE_HEADER = "message";

	public static final String STOMP_ACK_HEADER = "ack";

	public static final String STOMP_NACK_HEADER = "nack";

	public static final String STOMP_LOGIN_HEADER = "login";

	public static final String STOMP_PASSCODE_HEADER = "passcode";

	public static final String STOMP_DESTINATION_HEADER = "destination";

	public static final String STOMP_CONTENT_TYPE_HEADER = "content-type";

	public static final String STOMP_CONTENT_LENGTH_HEADER = "content-length";

	public static final String STOMP_HEARTBEAT_HEADER = "heart-beat";


	// Other header names

	public static final String COMMAND_HEADER = "stompCommand";


	private static final AtomicLong messageIdCounter = new AtomicLong();


	/**
	 * A constructor for creating new STOMP message headers.
	 */
	private StompHeaderAccessor(StompCommand command, Map<String, List<String>> externalSourceHeaders) {

		super(command.getMessageType(), externalSourceHeaders);

		Assert.notNull(command, "command is required");
		setHeader(COMMAND_HEADER, command);

		if (externalSourceHeaders != null) {
			setSimpMessageHeaders(command, externalSourceHeaders);
		}
	}

	private void setSimpMessageHeaders(StompCommand command, Map<String, List<String>> extHeaders) {

		List<String> values = extHeaders.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER);
		if (!CollectionUtils.isEmpty(values)) {
			super.setDestination(values.get(0));
		}

		values = extHeaders.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER);
		if (!CollectionUtils.isEmpty(values)) {
			super.setContentType(MediaType.parseMediaType(values.get(0)));
		}

		if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.UNSUBSCRIBE.equals(command)) {
			values = extHeaders.get(StompHeaderAccessor.STOMP_ID_HEADER);
			if (!CollectionUtils.isEmpty(values)) {
				super.setSubscriptionId(values.get(0));
			}
		}
		else if (StompCommand.MESSAGE.equals(command)) {
			values = extHeaders.get(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER);
			if (!CollectionUtils.isEmpty(values)) {
				super.setSubscriptionId(values.get(0));
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
			result.put(STOMP_DESTINATION_HEADER, Arrays.asList(destination));
		}

		MediaType contentType = getContentType();
		if (contentType != null) {
			result.put(STOMP_CONTENT_TYPE_HEADER, Arrays.asList(contentType.toString()));
		}

		if (StompCommand.MESSAGE.equals(getCommand())) {
			String subscriptionId = getSubscriptionId();
			if (subscriptionId != null) {
				result.put(STOMP_SUBSCRIPTION_HEADER, Arrays.asList(subscriptionId));
			}
			else {
				logger.warn("STOMP MESSAGE frame should have a subscription: " + this.toString());
			}
			if ((getMessageId() == null)) {
				String messageId = getSessionId() + "-" + messageIdCounter.getAndIncrement();
				result.put(STOMP_MESSAGE_ID_HEADER, Arrays.asList(messageId));
			}
		}

		return result;
	}

	public void setCommandIfNotSet(StompCommand command) {
		if (getCommand() == null) {
			setHeader(COMMAND_HEADER, command);
		}
	}

	public StompCommand getCommand() {
		return (StompCommand) getHeader(COMMAND_HEADER);
	}

	public Set<String> getAcceptVersion() {
		String rawValue = getFirstNativeHeader(STOMP_ACCEPT_VERSION_HEADER);
		return (rawValue != null) ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.<String>emptySet();
	}

	public void setAcceptVersion(String acceptVersion) {
		setNativeHeader(STOMP_ACCEPT_VERSION_HEADER, acceptVersion);
	}

	public void setHost(String host) {
		setNativeHeader(STOMP_HOST_HEADER, host);
	}

	public String getHost() {
		return getFirstNativeHeader(STOMP_HOST_HEADER);
	}

	@Override
	public void setDestination(String destination) {
		super.setDestination(destination);
		setNativeHeader(STOMP_DESTINATION_HEADER, destination);
	}

	public long[] getHeartbeat() {
		String rawValue = getFirstNativeHeader(STOMP_HEARTBEAT_HEADER);
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
			setNativeHeader(STOMP_CONTENT_TYPE_HEADER, mediaType.toString());
		}
	}

	public MediaType getContentType() {
		String value = getFirstNativeHeader(STOMP_CONTENT_TYPE_HEADER);
		return (value != null) ? MediaType.parseMediaType(value) : null;
	}

	public Integer getContentLength() {
		String contentLength = getFirstNativeHeader(STOMP_CONTENT_LENGTH_HEADER);
		return StringUtils.hasText(contentLength) ? new Integer(contentLength) : null;
	}

	public void setContentLength(int contentLength) {
		setNativeHeader(STOMP_CONTENT_LENGTH_HEADER, String.valueOf(contentLength));
	}

	public void setHeartbeat(long cx, long cy) {
		setNativeHeader(STOMP_HEARTBEAT_HEADER, StringUtils.arrayToCommaDelimitedString(new Object[] {cx, cy}));
	}

	public void setAck(String ack) {
		setNativeHeader(STOMP_ACK_HEADER, ack);
	}

	public String getAck() {
		return getFirstNativeHeader(STOMP_ACK_HEADER);
	}

	public void setNack(String nack) {
		setNativeHeader(STOMP_NACK_HEADER, nack);
	}

	public String getNack() {
		return getFirstNativeHeader(STOMP_NACK_HEADER);
	}

	public void setLogin(String login) {
		setNativeHeader(STOMP_LOGIN_HEADER, login);
	}

	public String getLogin() {
		return getFirstNativeHeader(STOMP_LOGIN_HEADER);
	}


	public void setPasscode(String passcode) {
		setNativeHeader(STOMP_PASSCODE_HEADER, passcode);
	}

	public String getPasscode() {
		return getFirstNativeHeader(STOMP_PASSCODE_HEADER);
	}

	public void setReceiptId(String receiptId) {
		setNativeHeader(STOMP_RECEIPT_ID_HEADER, receiptId);
	}

	public String getReceiptId() {
		return getFirstNativeHeader(STOMP_RECEIPT_ID_HEADER);
	}

	public String getMessage() {
		return getFirstNativeHeader(STOMP_MESSAGE_HEADER);
	}

	public void setMessage(String content) {
		setNativeHeader(STOMP_MESSAGE_HEADER, content);
	}

	public String getMessageId() {
		return getFirstNativeHeader(STOMP_MESSAGE_ID_HEADER);
	}

	public void setMessageId(String id) {
		setNativeHeader(STOMP_MESSAGE_ID_HEADER, id);
	}

	public String getVersion() {
		return getFirstNativeHeader(STOMP_VERSION_HEADER);
	}

	public void setVersion(String version) {
		setNativeHeader(STOMP_VERSION_HEADER, version);
	}

}
