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

package org.springframework.web.messaging.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.messaging.MessageType;


/**
 * A base class for working with message headers in Web, messaging protocols that support
 * the publish-subscribe message pattern. Provides uniform access to specific values
 * common across protocols such as a destination, message type (publish,
 * subscribe/unsubscribe), session id, and others.
 * <p>
 * Use one of the static factory method in this class, then call getters and setters, and
 * at the end if necessary call {@link #toMap()} to obtain the updated headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebMessageHeaderAccesssor extends NativeMessageHeaderAccessor {

	public static final String DESTINATIONS = "destinations";

	public static final String CONTENT_TYPE = "contentType";

	public static final String MESSAGE_TYPE = "messageType";

	public static final String PROTOCOL_MESSAGE_TYPE = "protocolMessageType";

	public static final String SESSION_ID = "sessionId";

	public static final String SUBSCRIPTION_ID = "subscriptionId";


	/**
	 * A constructor for creating new message headers.
	 * This constructor is protected. See factory methods in this and sub-classes.
	 */
	protected WebMessageHeaderAccesssor(MessageType messageType, Object protocolMessageType,
			Map<String, List<String>> externalSourceHeaders) {

		super(externalSourceHeaders);

		Assert.notNull(messageType, "messageType is required");
		setHeader(MESSAGE_TYPE, messageType);

		if (protocolMessageType != null) {
			setHeader(PROTOCOL_MESSAGE_TYPE, protocolMessageType);
		}
	}

	/**
	 * A constructor for accessing and modifying existing message headers. This
	 * constructor is protected. See factory methods in this and sub-classes.
	 */
	protected WebMessageHeaderAccesssor(Message<?> message) {
		super(message);
		Assert.notNull(message, "message is required");
	}


	/**
	 * Create {@link WebMessageHeaderAccesssor} for a new {@link Message} with
	 * {@link MessageType#MESSAGE}.
	 */
	public static WebMessageHeaderAccesssor create() {
		return new WebMessageHeaderAccesssor(MessageType.MESSAGE, null, null);
	}

	/**
	 * Create {@link WebMessageHeaderAccesssor} for a new {@link Message} of a specific type.
	 */
	public static WebMessageHeaderAccesssor create(MessageType messageType) {
		return new WebMessageHeaderAccesssor(messageType, null, null);
	}

	/**
	 * Create {@link WebMessageHeaderAccesssor} from the headers of an existing message.
	 */
	public static WebMessageHeaderAccesssor wrap(Message<?> message) {
		return new WebMessageHeaderAccesssor(message);
	}


	public MessageType getMessageType() {
		return (MessageType) getHeader(MESSAGE_TYPE);
	}

	protected void setProtocolMessageType(Object protocolMessageType) {
		setHeader(PROTOCOL_MESSAGE_TYPE, protocolMessageType);
	}

	protected Object getProtocolMessageType() {
		return getHeader(PROTOCOL_MESSAGE_TYPE);
	}

	public void setDestination(String destination) {
		Assert.notNull(destination, "destination is required");
		setHeader(DESTINATIONS, Arrays.asList(destination));
	}

	@SuppressWarnings("unchecked")
	public String getDestination() {
		List<String> destinations = (List<String>) getHeader(DESTINATIONS);
		return CollectionUtils.isEmpty(destinations) ? null : destinations.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<String> getDestinations() {
		List<String> destinations = (List<String>) getHeader(DESTINATIONS);
		return CollectionUtils.isEmpty(destinations) ? null : destinations;
	}

	public void setDestinations(List<String> destinations) {
		Assert.notNull(destinations, "destinations are required");
		setHeader(DESTINATIONS, destinations);
	}

	public MediaType getContentType() {
		return (MediaType) getHeader(CONTENT_TYPE);
	}

	public void setContentType(MediaType contentType) {
		Assert.notNull(contentType, "contentType is required");
		setHeader(CONTENT_TYPE, contentType);
	}

	public String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID);
	}

	public void setSubscriptionId(String subscriptionId) {
		setHeader(SUBSCRIPTION_ID, subscriptionId);
	}

	public String getSessionId() {
		return (String) getHeader(SESSION_ID);
	}

	public void setSessionId(String sessionId) {
		setHeader(SESSION_ID, sessionId);
	}

}
