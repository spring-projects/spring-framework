/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
