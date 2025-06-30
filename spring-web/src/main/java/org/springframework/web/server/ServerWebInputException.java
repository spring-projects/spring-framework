/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;

/**
 * Exception for errors that fit response status 400 (bad request) for use in
 * Spring Web applications. The exception provides additional fields (for example,
 * an optional {@link MethodParameter} if related to the error).
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ServerWebInputException extends ResponseStatusException {

	private final @Nullable MethodParameter parameter;


	/**
	 * Constructor with an explanation only.
	 */
	public ServerWebInputException(String reason) {
		this(reason, null, null);
	}

	/**
	 * Constructor for a 400 error linked to a specific {@code MethodParameter}.
	 */
	public ServerWebInputException(String reason, @Nullable MethodParameter parameter) {
		this(reason, parameter, null);
	}

	/**
	 * Constructor for a 400 error with a root cause.
	 */
	public ServerWebInputException(String reason, @Nullable MethodParameter parameter, @Nullable Throwable cause) {
		this(reason, parameter, cause, null, null);
	}

	/**
	 * Constructor with a message code and arguments for resolving the error
	 * "detail" via {@link org.springframework.context.MessageSource}.
	 * @since 6.0
	 */
	protected ServerWebInputException(String reason, @Nullable MethodParameter parameter, @Nullable Throwable cause,
			@Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

		super(HttpStatus.BAD_REQUEST, reason, cause, messageDetailCode, messageDetailArguments);
		this.parameter = parameter;
	}


	/**
	 * Return the {@code MethodParameter} associated with this error, if any.
	 */
	public @Nullable MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
