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
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;


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

	public static final String DESTINATION_HEADER = "destination";

	public static final String MESSAGE_TYPE_HEADER = "messageType";

	public static final String SESSION_ID_HEADER = "sessionId";

	public static final String SUBSCRIPTION_ID_HEADER = "subscriptionId";

	public static final String USER_HEADER = "user";


	/**
	 * A constructor for creating new message headers.
	 * This constructor is protected. See factory methods in this and sub-classes.
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType, Map<String, List<String>> externalSourceHeaders) {
		super(externalSourceHeaders);
		Assert.notNull(messageType, "messageType is required");
		setHeader(MESSAGE_TYPE_HEADER, messageType);
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
		return new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null);
	}

	/**
	 * Create {@link SimpMessageHeaderAccessor} for a new {@link Message} of a specific type.
	 */
	public static SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		return new SimpMessageHeaderAccessor(messageType, null);
	}

	/**
	 * Create {@link SimpMessageHeaderAccessor} from the headers of an existing message.
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}

	public void setMessageTypeIfNotSet(SimpMessageType messageType) {
		if (getMessageType() == null) {
			setHeader(MESSAGE_TYPE_HEADER, messageType);
		}
	}

	public SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE_HEADER);
	}

	public void setDestination(String destination) {
		Assert.notNull(destination, "destination is required");
		setHeader(DESTINATION_HEADER, destination);
	}

	public String getDestination() {
		return (String) getHeader(DESTINATION_HEADER);
	}

	public MediaType getContentType() {
		return (MediaType) getHeader(MessageHeaders.CONTENT_TYPE);
	}

	public void setContentType(MediaType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	public String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID_HEADER);
	}

	public void setSubscriptionId(String subscriptionId) {
		setHeader(SUBSCRIPTION_ID_HEADER, subscriptionId);
	}

	public String getSessionId() {
		return (String) getHeader(SESSION_ID_HEADER);
	}

	public void setSessionId(String sessionId) {
		setHeader(SESSION_ID_HEADER, sessionId);
	}

	public Principal getUser() {
		return (Principal) getHeader(USER_HEADER);
	}

	public void setUser(Principal principal) {
		setHeader(USER_HEADER, principal);
	}

}
