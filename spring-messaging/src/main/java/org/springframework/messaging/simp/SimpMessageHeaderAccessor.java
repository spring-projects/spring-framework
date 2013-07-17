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

package org.springframework.messaging.simp;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;


/**
 * A base class for working with message headers in simple messaging protocols that
 * support basic messaging patterns. Provides uniform access to specific values common
 * across protocols such as a destination, message type (e.g. publish, subscribe, etc),
 * session id, and others.
 * <p>
 * Use one of the static factory method in this class, then call getters and setters, and
 * at the end if necessary call {@link #toMap()} to obtain the updated headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	public static final String DESTINATIONS = "destinations";

	public static final String MESSAGE_TYPE = "messageType";

	// TODO
	public static final String PROTOCOL_MESSAGE_TYPE = "protocolMessageType";

	public static final String SESSION_ID = "sessionId";

	public static final String SUBSCRIPTION_ID = "subscriptionId";

	public static final String USER = "user";


	/**
	 * A constructor for creating new message headers.
	 * This constructor is protected. See factory methods in this and sub-classes.
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType, Object protocolMessageType,
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
	protected SimpMessageHeaderAccessor(Message<?> message) {
		super(message);
		Assert.notNull(message, "message is required");
	}


	/**
	 * Create {@link SimpMessageHeaderAccessor} for a new {@link Message} with
	 * {@link SimpMessageType#MESSAGE}.
	 */
	public static SimpMessageHeaderAccessor create() {
		return new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null, null);
	}

	/**
	 * Create {@link SimpMessageHeaderAccessor} for a new {@link Message} of a specific type.
	 */
	public static SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		return new SimpMessageHeaderAccessor(messageType, null, null);
	}

	/**
	 * Create {@link SimpMessageHeaderAccessor} from the headers of an existing message.
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}


	public SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE);
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
		return (MediaType) getHeader(MessageHeaders.CONTENT_TYPE);
	}

	public void setContentType(MediaType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
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

	public Principal getUser() {
		return (Principal) getHeader(USER);
	}

	public void setUser(Principal principal) {
		setHeader(USER, principal);
	}

}
