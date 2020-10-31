/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.socket;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Representation of WebSocket "close" status codes and reasons. Status codes
 * in the 1xxx range are pre-defined by the protocol.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">
 *     RFC 6455, Section 7.4.1 "Defined Status Codes"</a>
 */
public final class CloseStatus {

	/**
	 * "1000 indicates a normal closure, meaning that the purpose for which the connection
	 * was established has been fulfilled."
	 */
	public static final CloseStatus NORMAL = new CloseStatus(1000);

	/**
	 * "1001 indicates that an endpoint is "going away", such as a server going down or a
	 * browser having navigated away from a page."
	 */
	public static final CloseStatus GOING_AWAY = new CloseStatus(1001);

	/**
	 * "1002 indicates that an endpoint is terminating the connection due to a protocol
	 * error."
	 */
	public static final CloseStatus PROTOCOL_ERROR  = new CloseStatus(1002);

	/**
	 * "1003 indicates that an endpoint is terminating the connection because it has
	 * received a type of data it cannot accept (e.g., an endpoint that understands only
	 * text data MAY send this if it receives a binary message)."
	 */
	public static final CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003);

	// 10004: Reserved.
	// The specific meaning might be defined in the future.

	/**
	 * "1005 is a reserved value and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a status
	 * code to indicate that no status code was actually present."
	 */
	public static final CloseStatus NO_STATUS_CODE = new CloseStatus(1005);

	/**
	 * "1006 is a reserved value and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a status
	 * code to indicate that the connection was closed abnormally, e.g., without sending
	 * or receiving a Close control frame."
	 */
	public static final CloseStatus NO_CLOSE_FRAME = new CloseStatus(1006);

	/**
	 * "1007 indicates that an endpoint is terminating the connection because it has
	 * received data within a message that was not consistent with the type of the message
	 * (e.g., non-UTF-8 [RFC3629] data within a text message)."
	 */
	public static final CloseStatus BAD_DATA = new CloseStatus(1007);

	/**
	 * "1008 indicates that an endpoint is terminating the connection because it has
	 * received a message that violates its policy. This is a generic status code that can
	 * be returned when there is no other more suitable status code (e.g., 1003 or 1009)
	 * or if there is a need to hide specific details about the policy."
	 */
	public static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008);

	/**
	 * "1009 indicates that an endpoint is terminating the connection because it has
	 * received a message that is too big for it to process."
	 */
	public static final CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009);

	/**
	 * "1010 indicates that an endpoint (client) is terminating the connection because it
	 * has expected the server to negotiate one or more extension, but the server didn't
	 * return them in the response message of the WebSocket handshake. The list of
	 * extensions that are needed SHOULD appear in the /reason/ part of the Close frame.
	 * Note that this status code is not used by the server, because it can fail the
	 * WebSocket handshake instead."
	 */
	public static final CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010);

	/**
	 * "1011 indicates that a server is terminating the connection because it encountered
	 * an unexpected condition that prevented it from fulfilling the request."
	 */
	public static final CloseStatus SERVER_ERROR = new CloseStatus(1011);

	/**
	 * "1012 indicates that the service is restarted. A client may reconnect, and if it
	 * chooses to do, should reconnect using a randomized delay of 5 - 30s."
	 */
	public static final CloseStatus SERVICE_RESTARTED = new CloseStatus(1012);

	/**
	 * "1013 indicates that the service is experiencing overload. A client should only
	 * connect to a different IP (when there are multiple for the target) or reconnect to
	 * the same IP upon user action."
	 */
	public static final CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013);

	/**
	 * "1015 is a reserved value and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a status
	 * code to indicate that the connection was closed due to a failure to perform a TLS
	 * handshake (e.g., the server certificate can't be verified)."
	 */
	public static final CloseStatus TLS_HANDSHAKE_FAILURE = new CloseStatus(1015);


	private final int code;

	@Nullable
	private final String reason;


	/**
	 * Create a new {@link CloseStatus} instance.
	 * @param code the status code
	 */
	public CloseStatus(int code) {
		this(code, null);
	}

	/**
	 * Create a new {@link CloseStatus} instance.
	 * @param code the status code
	 * @param reason the reason
	 */
	public CloseStatus(int code, @Nullable String reason) {
		Assert.isTrue((code >= 1000 && code < 5000), "Invalid status code");
		this.code = code;
		this.reason = reason;
	}


	/**
	 * Return the status code.
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Return the reason, or {@code null} if none.
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}

	/**
	 * Create a new {@link CloseStatus} from this one with the specified reason.
	 * @param reason the reason
	 * @return a new {@link CloseStatus} instance
	 */
	public CloseStatus withReason(String reason) {
		Assert.hasText(reason, "Reason must not be empty");
		return new CloseStatus(this.code, reason);
	}

	/**
	 * @deprecated as of 5.3 in favor of comparing codes directly
	 */
	@Deprecated
	public boolean equalsCode(CloseStatus other) {
		return (this.code == other.code);
	}


	/**
	 * Return a constant for the given code, or create a new instance if the
	 * code does not match or there is a reason.
	 * @since 5.3
	 */
	public static CloseStatus create(int code, @Nullable String reason) {
		if (StringUtils.isEmpty(reason)) {
			switch (code) {
				case 1000:
					return NORMAL;
				case 1001:
					return GOING_AWAY;
				case 1002:
					return PROTOCOL_ERROR;
				case 1003:
					return NOT_ACCEPTABLE;
				case 1005:
					return NO_STATUS_CODE;
				case 1006:
					return NO_CLOSE_FRAME;
				case 1007:
					return BAD_DATA;
				case 1008:
					return POLICY_VIOLATION;
				case 1009:
					return TOO_BIG_TO_PROCESS;
				case 1010:
					return REQUIRED_EXTENSION;
				case 1011:
					return SERVER_ERROR;
				case 1012:
					return SERVICE_RESTARTED;
				case 1013:
					return SERVICE_OVERLOAD;
			}
		}
		return new CloseStatus(code, reason);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CloseStatus)) {
			return false;
		}
		CloseStatus otherStatus = (CloseStatus) other;
		return (this.code == otherStatus.code &&
				ObjectUtils.nullSafeEquals(this.reason, otherStatus.reason));
	}

	@Override
	public int hashCode() {
		return this.code * 29 + ObjectUtils.nullSafeHashCode(this.reason);
	}

	@Override
	public String toString() {
		return "CloseStatus[code=" + this.code + ", reason=" + this.reason + "]";
	}

}
