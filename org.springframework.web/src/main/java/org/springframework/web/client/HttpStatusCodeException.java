/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.http.HttpStatus;

/**
 * Abstract base class for exceptions based on an {@link HttpStatus}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class HttpStatusCodeException extends RestClientException {

	private final HttpStatus statusCode;

	private final String statusText;


	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on a {@link HttpStatus}.
	 * @param statusCode the status code
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		super(statusCode.toString());
		this.statusCode = statusCode;
		this.statusText = statusCode.name();
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on a {@link HttpStatus} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		super(statusCode.value() + " " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}


	/**
	 * Returns the HTTP status code.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Returns the HTTP status text.
	 */
	public String getStatusText() {
		return this.statusText;
	}

}
