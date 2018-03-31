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

package org.springframework.web.server;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

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

	@Nullable
	private final HandlerMethod handlerMethod;

	@Nullable
	private final MethodParameter parameter;


	/**
	 * Constructor with an explanation only.
	 */
	public ServerErrorException(String reason) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, null);
		this.handlerMethod = null;
		this.parameter = null;
	}

	/**
	 * Constructor with a reason and root cause.
	 * @since 5.0.5
	 */
	public ServerErrorException(String reason, Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = null;
		this.parameter = null;
	}

	/**
	 * Constructor for a 500 error with a {@link MethodParameter}.
	 */
	public ServerErrorException(String reason, MethodParameter parameter, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = null;
		this.parameter = parameter;
	}

	/**
	 * Constructor for a 500 error with a root cause.
	 */
	public ServerErrorException(String reason, HandlerMethod handlerMethod, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = handlerMethod;
		this.parameter = null;
	}

	/**
	 * Constructor for a 500 error linked to a specific {@code MethodParameter}.
	 * @deprecated in favor of {@link #ServerErrorException(String, MethodParameter, Throwable)}
	 */
	@Deprecated
	public ServerErrorException(String reason, MethodParameter parameter) {
		this(reason, parameter, null);
	}


	/**
	 * Return the controller method associated with the error, if any.
	 * @since 5.0.5
	 */
	@Nullable
	public HandlerMethod getHandlerMethod() {
		return this.handlerMethod;
	}

	/**
	 * Return the controller method argument associated with this error, if any.
	 */
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
