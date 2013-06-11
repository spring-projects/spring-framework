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

package org.springframework.web.messaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.CollectionUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PubSubHeaders {

	private static final String DESTINATIONS = "destinations";

	private static final String CONTENT_TYPE = "contentType";

	private static final String MESSAGE_TYPE = "messageType";

	private static final String SUBSCRIPTION_ID = "subscriptionId";

	private static final String PROTOCOL_MESSAGE_TYPE = "protocolMessageType";

	private static final String SESSION_ID = "sessionId";

	private static final String RAW_HEADERS = "rawHeaders";


	private final Map<String, Object> messageHeaders;

	private final Map<String, String> rawHeaders;


	/**
	 * Constructor for building new headers.
	 *
	 * @param messageType the message type
	 * @param protocolMessageType the protocol-specific message type or command
	 */
	public PubSubHeaders(MessageType messageType, Object protocolMessageType) {

		this.messageHeaders = new HashMap<String, Object>();
		this.messageHeaders.put(MESSAGE_TYPE, messageType);
		if (protocolMessageType != null) {
			this.messageHeaders.put(PROTOCOL_MESSAGE_TYPE, protocolMessageType);
		}

		this.rawHeaders = new HashMap<String, String>();
		this.messageHeaders.put(RAW_HEADERS, this.rawHeaders);
	}

	public PubSubHeaders() {
		this(MessageType.MESSAGE, null);
	}

	/**
	 * Constructor for access to existing {@link MessageHeaders}.
	 *
	 * @param messageHeaders
	 */
	@SuppressWarnings("unchecked")
	public PubSubHeaders(MessageHeaders messageHeaders, boolean readOnly) {

		this.messageHeaders = readOnly ? messageHeaders : new HashMap<String, Object>(messageHeaders);
		this.rawHeaders = this.messageHeaders.containsKey(RAW_HEADERS) ?
				(Map<String, String>) messageHeaders.get(RAW_HEADERS) : Collections.<String, String>emptyMap();

		if (this.messageHeaders.get(MESSAGE_TYPE) == null) {
			this.messageHeaders.put(MESSAGE_TYPE, MessageType.MESSAGE);
		}
	}


	public Map<String, Object> getMessageHeaders() {
		return this.messageHeaders;
	}

	public Map<String, String> getRawHeaders() {
		return this.rawHeaders;
	}

	public MessageType getMessageType() {
		return (MessageType) this.messageHeaders.get(MESSAGE_TYPE);
	}

	public void setProtocolMessageType(Object protocolMessageType) {
		this.messageHeaders.put(PROTOCOL_MESSAGE_TYPE, protocolMessageType);
	}

	public Object getProtocolMessageType() {
		return this.messageHeaders.get(PROTOCOL_MESSAGE_TYPE);
	}

	public void setDestination(String destination) {
		this.messageHeaders.put(DESTINATIONS, Arrays.asList(destination));
	}

	public String getDestination() {
		@SuppressWarnings("unchecked")
		List<String> destination = (List<String>) messageHeaders.get(DESTINATIONS);
		return CollectionUtils.isEmpty(destination) ? null : destination.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<String> getDestinations() {
		return (List<String>) messageHeaders.get(DESTINATIONS);
	}

	public void setDestinations(List<String> destinations) {
		if (destinations != null) {
			this.messageHeaders.put(DESTINATIONS, destinations);
		}
	}

	public MediaType getContentType() {
		return (MediaType) this.messageHeaders.get(CONTENT_TYPE);
	}

	public void setContentType(MediaType mediaType) {
		if (mediaType != null) {
			this.messageHeaders.put(CONTENT_TYPE, mediaType);
		}
	}

	public String getSubscriptionId() {
		return (String) this.messageHeaders.get(SUBSCRIPTION_ID);
	}

	public void setSubscriptionId(String subscriptionId) {
		this.messageHeaders.put(SUBSCRIPTION_ID, subscriptionId);
	}

	public String getSessionId() {
		return (String) this.messageHeaders.get(SESSION_ID);
	}

	public void setSessionId(String sessionId) {
		this.messageHeaders.put(SESSION_ID, sessionId);
	}

}
