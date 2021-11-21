/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.server;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for exceptions associated with specific HTTP response status codes.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ResponseStatusException extends NestedRuntimeException {

	private final int status;

	@Nullable
	private final String reason;


	/**
	 * Constructor with a response status.
	 * @param status the HTTP status (required)
	 */
	public ResponseStatusException(HttpStatus status) {
		this(status, null);
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation.
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason) {
		super("");
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation, as well as a nested exception.
	 * @param status the HTTP status (required)
	 * @param reason the associated reason (optional)
	 * @param cause a nested exception (optional)
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * Constructor with a response status and a reason to add to the exception
	 * message as explanation, as well as a nested exception.
	 * @param rawStatusCode the HTTP status code value
	 * @param reason the associated reason (optional)
	 * @param cause a nested exception (optional)
	 * @since 5.3
	 */
	public ResponseStatusException(int rawStatusCode, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		this.status = rawStatusCode;
		this.reason = reason;
	}


	/**
	 * Return the HTTP status associated with this exception.
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	public HttpStatus getStatus() {
		return HttpStatus.valueOf(this.status);
	}

	/**
	 * Return the HTTP status code (potentially non-standard and not resolvable
	 * through the {@link HttpStatus} enum) as an integer.
	 * @return the HTTP status as an integer value
	 * @since 5.3
	 * @see #getStatus()
	 * @see HttpStatus#resolve(int)
	 */
	public int getRawStatusCode() {
		return this.status;
	}

	/**
	 * Return headers associated with the exception that should be added to the
	 * error response, e.g. "Allow", "Accept", etc.
	 * <p>The default implementation in this class returns empty headers.
	 * @since 5.1.13
	 */
	public HttpHeaders getResponseHeaders() {
		return HttpHeaders.EMPTY;
	}

	/**
	 * The reason explaining the exception (potentially {@code null} or empty).
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}


	@Override
	public String getMessage() {
		HttpStatus code = HttpStatus.resolve(this.status);
		String msg = (code != null ? code : this.status) + (this.reason != null ? " \"" + this.reason + "\"" : "");
		return NestedExceptionUtils.buildMessage(msg, getCause());
	}

}
