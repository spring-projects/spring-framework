/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

	private final @Nullable HttpInputMessage httpInputMessage;


	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 * @param httpInputMessage the original HTTP message
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, HttpInputMessage httpInputMessage) {
		super(msg);
		this.httpInputMessage = httpInputMessage;
	}

	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 * @param httpInputMessage the original HTTP message
	 * @since 5.1
	 */
	public HttpMessageNotReadableException(String msg, @Nullable Throwable cause, HttpInputMessage httpInputMessage) {
		super(msg, cause);
		this.httpInputMessage = httpInputMessage;
	}


	/**
	 * Return the original HTTP message.
	 * @since 5.1
	 */
	public HttpInputMessage getHttpInputMessage() {
		Assert.state(this.httpInputMessage != null, "No HttpInputMessage available");
		return this.httpInputMessage;
	}

}
