/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions thrown by {@link RestClient} and {@link RestTemplate}
 * in case a request fails because of a server error response, a failure to decode
 * the response, or a low level I/O error.
 *
 * <p>Server error responses are determined by
 * {@link RestClient.ResponseSpec#onStatus status handlers} for {@code RestClient},
 * and by {@link ResponseErrorHandler} for {@code RestTemplate}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class RestClientException extends NestedRuntimeException {

	private static final long serialVersionUID = -4084444984163796577L;


	/**
	 * Construct a new instance of {@code RestClientException} with the given message.
	 * @param msg the message
	 */
	public RestClientException(String msg) {
		super(msg);
	}

	/**
	 * Construct a new instance of {@code RestClientException} with the given message and
	 * exception.
	 * @param msg the message
	 * @param ex the exception
	 */
	public RestClientException(String msg, @Nullable Throwable ex) {
		super(msg, ex);
	}

}
