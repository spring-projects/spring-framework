/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Base class for exceptions associated with specific HTTP response status codes.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseStatusException extends NestedRuntimeException {

	private final HttpStatus status;

	private final String reason;


	/**
	 * Constructor with a response code and a reason to add to the exception
	 * message as explanation.
	 */
	public ResponseStatusException(HttpStatus status, String reason) {
		this(status, reason, null);
	}

	/**
	 * Constructor with a nested exception.
	 */
	public ResponseStatusException(HttpStatus status, String reason, Throwable cause) {
		super("Request failure [status: " + status + ", reason: \"" + reason + "\"]", cause);
		Assert.notNull(status, "'status' is required");
		Assert.notNull(reason, "'reason' is required");
		this.status = status;
		this.reason = reason;
	}


	/**
	 * The HTTP status that fits the exception.
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * The reason explaining the exception.
	 */
	public String getReason() {
		return this.reason;
	}

}
