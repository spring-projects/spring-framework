/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#write} method fails.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageNotWritableException extends HttpMessageConversionException {

	/**
	 * Create a new HttpMessageNotWritableException.
	 * @param msg the detail message
	 */
	public HttpMessageNotWritableException(String msg) {
		super(msg);
	}

	/**
	 * Create a new HttpMessageNotWritableException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public HttpMessageNotWritableException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
