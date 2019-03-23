/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A base class for working with message headers in simple messaging protocols that
 * support basic messaging patterns. Provides uniform access to specific values common
 * across protocols such as a destination, message type (e.g. publish, subscribe, etc),
 * session id, and others.
 *
 * <p>Use one of the static factory method in this class, then call getters and setters,
 * and at the end if necessary call {@link #toMap()} to obtain the updated headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	private static final IdTimestampMessageHeaderInitializer headerInitializer;

	static {
		headerInitializer = new IdTimestampMessageHeaderInitializer();
		headerInitializer.setDisableIdGeneration();
		headerInitializer.setEnableTimestamp(false);
	}

	// SiMP header names

	public static final String DESTINATION_HEADER = "simpDestination";

	public static final String MESSAGE_TYPE_HEADER = "simpMessageType";

	public static final String SESSION_ID_HEADER = "simpSessionId";

	public static final String SESSION_ATTRIBUTES = "simpSessionAttributes";

	public static final String SUBSCRIPTION_ID_HEADER = "simpSubscriptionId";

	public static final String USER_HEADER = "simpUser";

	public static final String CONNECT_MESSAGE_HEADER = "simpConnectMessage";

	public static final String DISCONNECT_MESSAGE_HEADER = "simpDisconnectMessage";

	public static final String HEART_BEAT_HEADER = "simpHeartbeat";


	/**
	 * A header for internal use with "user" destinations where we need to
	 * restore the destination prior to sending messages to clients.
	 */
	public static final String ORIGINAL_DESTINATION = "simpOrigDestination";

	/**
	 * A header that indicates to the broker that the sender will ignore errors.
	 * The header is simply checked for presence or absence.
	 */
	public static final String IGNORE_ERROR = "simpIgnoreError";


	/**
	 * A constructor for creating new message headers.
	 * This constructor is protected. See factory methods in this and sub-classes.
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType,
			@Nullable Map<String, List<String>> externalSourceHeaders) {

		super(externalSourceHeaders);
		Assert.notNull(messageType, "MessageType must not be null");
		setHeader(MESSAGE_TYPE_HEADER, messageType);
		headerInitializer.initHeaders(this);
	}

	/**
	 * A constructor for accessing and modifying existing message headers. This
	 * constructor is protected. See factory methods in this and sub-classes.
	 */
	protected SimpMessageHeaderAccessor(Message<?> message) {
		super(message);
		headerInitializer.initHeaders(this);
	}


	@Override
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return wrap(message);
	}

	public void setMessageTypeIfNotSet(SimpMessageType messageType) {
		if (getMessageType() == null) {
			setHeader(MESSAGE_TYPE_HEADER, messageType);
		}
	}

	@Nullable
	public SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE_HEADER);
	}

	public void setDestination(@Nullable String destination) {
		setHeader(DESTINATION_HEADER, destination);
	}

	@Nullable
	public String getDestination() {
		return (String) getHeader(DESTINATION_HEADER);
	}

	public void setSubscriptionId(@Nullable String subscriptionId) {
		setHeader(SUBSCRIPTION_ID_HEADER, subscriptionId);
	}

	@Nullable
	public String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID_HEADER);
	}

	public void setSessionId(@Nullable String sessionId) {
		setHeader(SESSION_ID_HEADER, sessionId);
	}

	/**
	 * Return the id of the current session.
	 */
	@Nullable
	public String getSessionId() {
		return (String) getHeader(SESSION_ID_HEADER);
	}

	/**
	 * A static alternative for access to the session attributes header.
	 */
	public void setSessionAttributes(@Nullable Map<String, Object> attributes) {
		setHeader(SESSION_ATTRIBUTES, attributes);
	}

	/**
	 * Return the attributes associated with the current session.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public Map<String, Object> getSessionAttributes() {
		return (Map<String, Object>) getHeader(SESSION_ATTRIBUTES);
	}

	public void setUser(@Nullable Principal principal) {
		setHeader(USER_HEADER, principal);
	}

	/**
	 * Return the user associated with the current session.
	 */
	@Nullable
	public Principal getUser() {
		return (Principal) getHeader(USER_HEADER);
	}

	@Override
	public String getShortLogMessage(Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes[").append(getSessionAttributes().size()).append("]");
		}
		sb.append(getShortPayloadLogMessage(payload));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getDetailedLogMessage(@Nullable Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes=").append(getSessionAttributes());
		}
		if (!CollectionUtils.isEmpty((Map<String, List<String>>) getHeader(NATIVE_HEADERS))) {
			sb.append(" nativeHeaders=").append(getHeader(NATIVE_HEADERS));
		}
		sb.append(getDetailedPayloadLogMessage(payload));
		return sb.toString();
	}

	private StringBuilder getBaseLogMessage() {
		StringBuilder sb = new StringBuilder();
		SimpMessageType messageType = getMessageType();
		sb.append(messageType != null ? messageType.name() : SimpMessageType.OTHER);
		String destination = getDestination();
		if (destination != null) {
			sb.append(" destination=").append(destination);
		}
		String subscriptionId = getSubscriptionId();
		if (subscriptionId != null) {
			sb.append(" subscriptionId=").append(subscriptionId);
		}
		sb.append(" session=").append(getSessionId());
		Principal user = getUser();
		if (user != null) {
			sb.append(" user=").append(user.getName());
		}
		return sb;
	}


	// Static factory methods and accessors

	/**
	 * Create an instance with
	 * {@link org.springframework.messaging.simp.SimpMessageType} {@code MESSAGE}.
	 */
	public static SimpMessageHeaderAccessor create() {
		return new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null);
	}

	/**
	 * Create an instance with the given
	 * {@link org.springframework.messaging.simp.SimpMessageType}.
	 */
	public static SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		return new SimpMessageHeaderAccessor(messageType, null);
	}

	/**
	 * Create an instance from the payload and headers of the given Message.
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}

	@Nullable
	public static SimpMessageType getMessageType(Map<String, Object> headers) {
		return (SimpMessageType) headers.get(MESSAGE_TYPE_HEADER);
	}

	@Nullable
	public static String getDestination(Map<String, Object> headers) {
		return (String) headers.get(DESTINATION_HEADER);
	}

	@Nullable
	public static String getSubscriptionId(Map<String, Object> headers) {
		return (String) headers.get(SUBSCRIPTION_ID_HEADER);
	}

	@Nullable
	public static String getSessionId(Map<String, Object> headers) {
		return (String) headers.get(SESSION_ID_HEADER);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static Map<String, Object> getSessionAttributes(Map<String, Object> headers) {
		return (Map<String, Object>) headers.get(SESSION_ATTRIBUTES);
	}

	@Nullable
	public static Principal getUser(Map<String, Object> headers) {
		return (Principal) headers.get(USER_HEADER);
	}

	@Nullable
	public static long[] getHeartbeat(Map<String, Object> headers) {
		return (long[]) headers.get(HEART_BEAT_HEADER);
	}

}
