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

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;

/**
 * Exception for an {@link HttpStatus#INTERNAL_SERVER_ERROR} that exposes extra
 * information about a controller method that failed, or a controller method
 * argument that could not be resolved.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ServerErrorException extends ResponseStatusException {

	private final @Nullable Method handlerMethod;

	private final @Nullable MethodParameter parameter;


	/**
	 * Constructor for a 500 error with a reason and an optional cause.
	 * @since 5.0.5
	 */
	public ServerErrorException(String reason, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause, null, new Object[] {reason});
		this.handlerMethod = null;
		this.parameter = null;
	}

	/**
	 * Constructor for a 500 error with a handler {@link Method} and an optional cause.
	 * @since 5.0.5
	 */
	public ServerErrorException(String reason, Method handlerMethod, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause, null, new Object[] {reason});
		this.handlerMethod = handlerMethod;
		this.parameter = null;
	}

	/**
	 * Constructor for a 500 error with a {@link MethodParameter} and an optional cause.
	 */
	public ServerErrorException(String reason, MethodParameter parameter, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause, null, new Object[] {reason});
		this.handlerMethod = parameter.getMethod();
		this.parameter = parameter;
	}


	/**
	 * Return the handler method associated with the error, if any.
	 * @since 5.0.5
	 */
	public @Nullable Method getHandlerMethod() {
		return this.handlerMethod;
	}

	/**
	 * Return the specific method parameter associated with the error, if any.
	 */
	public @Nullable MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
