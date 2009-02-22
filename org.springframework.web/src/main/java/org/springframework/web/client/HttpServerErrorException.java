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

import org.springframework.web.http.HttpStatus;

/**
 * Exception thrown when a HTTP 5xx is received.
 *
 * @author Arjen Poutsma
 * @see org.springframework.web.client.core.SimpleHttpErrorHandler
 * @since 3.0
 */
public class HttpServerErrorException extends HttpStatusCodeException {

	/**
	 * Constructs a new instance of {@code HttpServerErrorException} based on a {@link HttpStatus}.
	 *
	 * @param statusCode the status code
	 */
	public HttpServerErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * Constructs a new instance of {@code HttpServerErrorException} based on a {@link HttpStatus} and status text.
	 *
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

}
