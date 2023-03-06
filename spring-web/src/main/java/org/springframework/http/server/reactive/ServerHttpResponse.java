/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an {@link HttpStatusCode} value
	 * @return {@code false} if the status code change wasn't processed because
	 * the HTTP response is committed, {@code true} if successfully set.
	 */
	boolean setStatusCode(@Nullable HttpStatusCode status);

	/**
	 * Return the status code that has been set, or otherwise fall back on the
	 * status of the response from the underlying server. The return value may
	 * be {@code null} if there is no default value from the
	 * underlying server.
	 */
	@Nullable
	HttpStatusCode getStatusCode();

	/**
	 * Set the HTTP status code to the given value as an integer.
	 * @param value the status code value
	 * @return {@code false} if the status code change wasn't processed because
	 * the HTTP response is committed, {@code true} if successfully set.
	 * @since 5.2.4
	 */
	default boolean setRawStatusCode(@Nullable Integer value) {
		return setStatusCode(value != null ? HttpStatusCode.valueOf(value) : null);
	}

	/**
	 * Return the status code that has been set, or otherwise fall back on the
	 * status of the response from the underlying server. The return value may
	 * be {@code null} if there is no default value from the underlying server.
	 * @since 5.2.4
	 * @deprecated as of 6.0, in favor of {@link #getStatusCode()}
	 */
	@Deprecated(since = "6.0")
	@Nullable
	default Integer getRawStatusCode() {
		HttpStatusCode httpStatus = getStatusCode();
		return (httpStatus != null ? httpStatus.value() : null);
	}

	/**
	 * Return a mutable map with the cookies to send to the server.
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

	/**
	 * Add the given {@code ResponseCookie}.
	 * @param cookie the cookie to add
	 * @throws IllegalStateException if the response has already been committed
	 */
	void addCookie(ResponseCookie cookie);

}
