/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Abstract base class for exception published by {@link WebClient} in case of errors.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class WebClientException extends NestedRuntimeException {

	private static final long serialVersionUID = 472776714118912855L;

	/**
	 * Construct a new instance of {@code WebClientException} with the given message.
	 * @param msg the message
	 */
	public WebClientException(String msg) {
		super(msg);
	}

	/**
	 * Construct a new instance of {@code WebClientException} with the given message
	 * and exception.
	 * @param msg the message
	 * @param ex the exception
	 */
	public WebClientException(@Nullable String msg, Throwable ex) {
		super(msg, ex);
	}

}
