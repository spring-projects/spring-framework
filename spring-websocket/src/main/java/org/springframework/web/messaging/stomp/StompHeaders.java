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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.messaging.PubSubHeaders;

import reactor.util.Assert;


/**
 * STOMP adapter for {@link MessageHeaders}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaders extends PubSubHeaders {

	private static final String ID = "id";

	private static final String HOST = "host";

	private static final String ACCEPT_VERSION = "accept-version";

	private static final String MESSAGE_ID = "message-id";

	private static final String RECEIPT_ID = "receipt-id";

	private static final String SUBSCRIPTION = "subscription";

	private static final String VERSION = "version";

	private static final String MESSAGE = "message";

	private static final String ACK = "ack";

	private static final String DESTINATION = "destination";

	private static final String CONTENT_TYPE = "content-type";

	private static final String CONTENT_LENGTH = "content-length";

	private static final String HEARTBEAT = "heart-beat";


	/**
	 * Constructor for building new headers.
	 *
	 * @param command the STOMP command
	 */
	public StompHeaders(StompCommand command) {
		super(command.getMessageType(), command);
	}

	/**
	 * Constructor for access to existing {@link MessageHeaders}.
	 *
	 * @param messageHeaders the existing message headers
	 * @param readOnly whether the resulting instance will be used for read-only access,
	 *        if {@code true}, then set methods will throw exceptions; if {@code false}
	 *        they will work.
	 */
	public StompHeaders(MessageHeaders messageHeaders, boolean readOnly) {
		super(messageHeaders, readOnly);
	}

	@Override
	public StompCommand getProtocolMessageType() {
		return (StompCommand) super.getProtocolMessageType();
	}

	public StompCommand getStompCommand() {
		return (StompCommand) super.getProtocolMessageType();
	}

	public Set<String> getAcceptVersion() {
		String rawValue = getRawHeaders().get(ACCEPT_VERSION);
		return (rawValue != null) ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.<String>emptySet();
	}

	public void setAcceptVersion(String acceptVersion) {
		getRawHeaders().put(ACCEPT_VERSION, acceptVersion);
	}

	@Override
	public void setDestination(String destination) {
		if (destination != null) {
			super.setDestination(destination);
			getRawHeaders().put(DESTINATION, destination);
		}
	}

	@Override
	public void setDestinations(List<String> destinations) {
		if (destinations != null) {
			super.setDestinations(destinations);
			getRawHeaders().put(DESTINATION, destinations.get(0));
		}
	}

	public long[] getHeartbeat() {
		String rawValue = getRawHeaders().get(HEARTBEAT);
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
			getRawHeaders().put(CONTENT_TYPE, mediaType.toString());
		}
	}

	public Integer getContentLength() {
		String contentLength = getRawHeaders().get(CONTENT_LENGTH);
		return StringUtils.hasText(contentLength) ? new Integer(contentLength) : null;
	}

	public void setContentLength(int contentLength) {
		getRawHeaders().put(CONTENT_LENGTH, String.valueOf(contentLength));
	}

	@Override
	public String getSubscriptionId() {
		return StompCommand.SUBSCRIBE.equals(getStompCommand()) ? getRawHeaders().get(ID) : null;
	}

	@Override
	public void setSubscriptionId(String subscriptionId) {
		Assert.isTrue(StompCommand.MESSAGE.equals(getStompCommand()),
				"\"subscription\" can only be set on a STOMP MESSAGE frame");
		super.setSubscriptionId(subscriptionId);
		getRawHeaders().put(SUBSCRIPTION, subscriptionId);
	}

	public void setHeartbeat(long cx, long cy) {
		getRawHeaders().put(HEARTBEAT, StringUtils.arrayToCommaDelimitedString(new Object[] {cx, cy}));
	}

	public String getMessage() {
		return getRawHeaders().get(MESSAGE);
	}

	public void setMessage(String content) {
		getRawHeaders().put(MESSAGE, content);
	}

	public String getMessageId() {
		return getRawHeaders().get(MESSAGE_ID);
	}

	public void setMessageId(String id) {
		getRawHeaders().put(MESSAGE_ID, id);
	}

	public String getVersion() {
		return getRawHeaders().get(VERSION);
	}

	public void setVersion(String version) {
		getRawHeaders().put(VERSION, version);
	}


	/**
	 * Update generic message headers from raw headers. This method only needs to be
	 * invoked when raw headers are added via {@link #getRawHeaders()}.
	 */
	public void updateMessageHeaders() {
		String destination = getRawHeaders().get(DESTINATION);
		if (destination != null) {
			setDestination(destination);
		}
		String contentType = getRawHeaders().get(CONTENT_TYPE);
		if (contentType != null) {
			setContentType(MediaType.parseMediaType(contentType));
		}
		if (StompCommand.SUBSCRIBE.equals(getStompCommand())) {
			if (getRawHeaders().get(ID) != null) {
				super.setSubscriptionId(getRawHeaders().get(ID));
			}
		}
	}

	/**
	 * Update raw headers from generic message headers. This method only needs to be
	 * invoked if creating {@link StompHeaders} from {@link MessageHeaders} that never
	 * contained raw headers.
	 */
	public void updateRawHeaders() {
		String destination = getDestination();
		if (destination != null) {
			getRawHeaders().put(DESTINATION, destination);
		}
		MediaType contentType = getContentType();
		if (contentType != null) {
			getRawHeaders().put(CONTENT_TYPE, contentType.toString());
		}
		String subscriptionId = getSubscriptionId();
		if (subscriptionId != null) {
			getRawHeaders().put(SUBSCRIPTION, subscriptionId);
		}
	}

}
