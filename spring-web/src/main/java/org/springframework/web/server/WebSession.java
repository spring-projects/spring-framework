/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.server;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Main contract for using a server-side session that provides access to session
 * attributes across HTTP requests.
 *
 * <p>The creation of a {@code WebSession} instance does not automatically start
 * a session thus causing the session id to be sent to the client (typically via
 * a cookie). A session starts implicitly when session attributes are added.
 * A session may also be created explicitly via {@link #start()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSession {

	/**
	 * Return a unique session identifier.
	 */
	String getId();

	/**
	 * Return a map that holds session attributes.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Return the session attribute value if present.
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * Return the session attribute value or if not present raise an
	 * {@link IllegalArgumentException}.
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, () -> "Required attribute '" + name + "' is missing.");
		return value;
	}

	/**
	 * Return the session attribute value, or a default, fallback value.
	 * @param name the attribute name
	 * @param defaultValue a default value to return instead
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * Force the creation of a session causing the session id to be sent when
	 * {@link #save()} is called.
	 */
	void start();

	/**
	 * Whether a session with the client has been started explicitly via
	 * {@link #start()} or implicitly by adding session attributes.
	 * If "false" then the session id is not sent to the client and the
	 * {@link #save()} method is essentially a no-op.
	 */
	boolean isStarted();

	/**
	 * Generate a new id for the session and update the underlying session
	 * storage to reflect the new id. After a successful call {@link #getId()}
	 * reflects the new session id.
	 * @return completion notification (success or error)
	 */
	Mono<Void> changeSessionId();

	/**
	 * Invalidate the current session and clear session storage.
	 * @return completion notification (success or error)
	 */
	Mono<Void> invalidate();

	/**
	 * Save the session through the {@code WebSessionStore} as follows:
	 * <ul>
	 * <li>If the session is new (i.e. created but never persisted), it must have
	 * been started explicitly via {@link #start()} or implicitly by adding
	 * attributes, or otherwise this method should have no effect.
	 * <li>If the session was retrieved through the {@code WebSessionStore},
	 * the implementation for this method must check whether the session was
	 * {@link #invalidate() invalidated} and if so return an error.
	 * </ul>
	 * <p>Note that this method is not intended for direct use by applications.
	 * Instead it is automatically invoked just before the response is
	 * committed.
	 * @return {@code Mono} to indicate completion with success or error
	 */
	Mono<Void> save();

	/**
	 * Return {@code true} if the session expired after {@link #getMaxIdleTime()
	 * maxIdleTime} elapsed.
	 * <p>Typically expiration checks should be automatically made when a session
	 * is accessed, a new {@code WebSession} instance created if necessary, at
	 * the start of request processing so that applications don't have to worry
	 * about expired session by default.
	 */
	boolean isExpired();

	/**
	 * Return the time when the session was created.
	 */
	Instant getCreationTime();

	/**
	 * Return the last time of session access as a result of user activity such
	 * as an HTTP request. Together with {@link #getMaxIdleTime()
	 * maxIdleTimeInSeconds} this helps to determine when a session is
	 * {@link #isExpired() expired}.
	 */
	Instant getLastAccessTime();

	/**
	 * Configure the max amount of time that may elapse after the
	 * {@link #getLastAccessTime() lastAccessTime} before a session is considered
	 * expired. A negative value indicates the session should not expire.
	 */
	void setMaxIdleTime(Duration maxIdleTime);

	/**
	 * Return the maximum time after the {@link #getLastAccessTime()
	 * lastAccessTime} before a session expires. A negative time indicates the
	 * session doesn't expire.
	 */
	Duration getMaxIdleTime();

}
