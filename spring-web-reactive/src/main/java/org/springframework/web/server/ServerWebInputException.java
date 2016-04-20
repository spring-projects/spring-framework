/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;

/**
 * Exception for errors that fit response status 400 (bad request) for use in
 * Spring Web applications. The exception provides additional fields (e.g.
 * an optional {@link MethodParameter} if related to the error).
 *
 * @author Rossen Stoyanchev
 */
public class ServerWebInputException extends ResponseStatusException {

	private final MethodParameter parameter;


	/**
	 * Constructor with an explanation only.
	 */
	public ServerWebInputException(String reason) {
		this(reason, null);
	}

	/**
	 * Constructor for a 400 error linked to a specific {@code MethodParameter}.
	 */
	public ServerWebInputException(String reason, MethodParameter parameter) {
		this(reason, parameter, null);
	}

	/**
	 * Constructor for a 400 error with a root cause.
	 */
	public ServerWebInputException(String reason, MethodParameter parameter, Throwable cause) {
		super(HttpStatus.BAD_REQUEST, reason, cause);
		this.parameter = parameter;
	}


	/**
	 * Return the {@code MethodParameter} associated with this error, if any.
	 */
	public Optional<MethodParameter> getMethodParameter() {
		return Optional.ofNullable(this.parameter);
	}

}
