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
package org.springframework.web;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

/**
 * Exception wrapper to associate an exception with a status code at runtime.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseStatusException extends NestedRuntimeException {

	private final HttpStatus httpStatus;


	public ResponseStatusException(HttpStatus status) {
		this(status, null);
	}

	public ResponseStatusException(HttpStatus status, Throwable cause) {
		super("Request processing failure with status code: " + status, cause);
		this.httpStatus = status;
	}


	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}
