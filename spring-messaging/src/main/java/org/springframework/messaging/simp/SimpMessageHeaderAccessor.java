/*
 * Copyright 2002-present the original author or authors.
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
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A base class for working with message headers in simple messaging protocols that
 * support basic messaging patterns. Provides uniform access to specific values common
 * across protocols such as a destination, message type (for example, publish, subscribe, etc),
 * session ID, and others.
 *
 * <p>Use one of the static factory methods in this class, then call getters and setters,
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


	private @Nullable Consumer<Principal> userCallback;


	/**
	 * A constructor for creating new message headers.
	 * <p>This constructor is protected. See factory methods in this class
	 * and subclasses.
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType,
			@Nullable Map<String, List<String>> externalSourceHeaders) {

		super(externalSourceHeaders);
		Assert.notNull(messageType, "MessageType must not be null");
		setHeader(MESSAGE_TYPE_HEADER, messageType);
		headerInitializer.initHeaders(this);
	}

	/**
	 * A constructor for accessing and modifying existing message headers.
	 * <p>This constructor is protected. See factory methods in this class
	 * and subclasses.
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

	public @Nullable SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE_HEADER);
	}

	public void setDestination(@Nullable String destination) {
		setHeader(DESTINATION_HEADER, destination);
	}

	public @Nullable String getDestination() {
		return (String) getHeader(DESTINATION_HEADER);
	}

	public void setSubscriptionId(@Nullable String subscriptionId) {
		setHeader(SUBSCRIPTION_ID_HEADER, subscriptionId);
	}

	public @Nullable String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID_HEADER);
	}

	public void setSessionId(@Nullable String sessionId) {
		setHeader(SESSION_ID_HEADER, sessionId);
	}

	/**
	 * Return the id of the current session.
	 */
	public @Nullable String getSessionId() {
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
	public @Nullable Map<String, Object> getSessionAttributes() {
		return (Map<String, Object>) getHeader(SESSION_ATTRIBUTES);
	}

	public void setUser(@Nullable Principal principal) {
		setHeader(USER_HEADER, principal);
		if (this.userCallback != null) {
			this.userCallback.accept(principal);
		}
	}

	/**
	 * Return the user associated with the current session.
	 */
	public @Nullable Principal getUser() {
		return (Principal) getHeader(USER_HEADER);
	}

	/**
	 * Provide a callback to be invoked if and when {@link #setUser(Principal)}
	 * is called. This is used internally on the inbound channel to detect
	 * token-based authentications through an interceptor.
	 * @param callback the callback to invoke
	 * @since 5.1.9
	 */
	public void setUserChangeCallback(Consumer<Principal> callback) {
		Assert.notNull(callback, "'callback' is required");
		this.userCallback = this.userCallback != null ? this.userCallback.andThen(callback) : callback;
	}

	@Override
	public String getShortLogMessage(Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes[").append(getSessionAttributes().size()).append(']');
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
	 * Create an instance by copying the headers of a Message.
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}

	public static @Nullable SimpMessageType getMessageType(Map<String, Object> headers) {
		return (SimpMessageType) headers.get(MESSAGE_TYPE_HEADER);
	}

	public static @Nullable String getDestination(Map<String, Object> headers) {
		return (String) headers.get(DESTINATION_HEADER);
	}

	public static @Nullable String getSubscriptionId(Map<String, Object> headers) {
		return (String) headers.get(SUBSCRIPTION_ID_HEADER);
	}

	public static @Nullable String getSessionId(Map<String, Object> headers) {
		return (String) headers.get(SESSION_ID_HEADER);
	}

	@SuppressWarnings("unchecked")
	public static @Nullable Map<String, Object> getSessionAttributes(Map<String, Object> headers) {
		return (Map<String, Object>) headers.get(SESSION_ATTRIBUTES);
	}

	public static @Nullable Principal getUser(Map<String, Object> headers) {
		return (Principal) headers.get(USER_HEADER);
	}

	public static long @Nullable [] getHeartbeat(Map<String, Object> headers) {
		return (long[]) headers.get(HEART_BEAT_HEADER);
	}

}
