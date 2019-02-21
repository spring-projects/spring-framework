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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Represents a reactive server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * Set the HTTP status code of the response.
	 * @param status the HTTP status as an {@link HttpStatus} enum value
	 * @return {@code false} if the status code has not been set because the
	 * HTTP response is already committed, {@code true} if successfully set.
	 */
	boolean setStatusCode(@Nullable HttpStatus status);

	/**
	 * Return the status code set via {@link #setStatusCode}, or if the status
	 * has not been set, return the default status code from the underlying
	 * server response. The return value may be {@code null} if the status code
	 * value is outside the {@link HttpStatus} enum range, or if the underlying
	 * server response does not have a default value.
	 */
	@Nullable
	HttpStatus getStatusCode();

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
