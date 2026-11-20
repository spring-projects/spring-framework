/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception thrown when multipart resolution fails.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see MultipartResolver#resolveMultipart
 * @see org.springframework.web.multipart.support.MultipartFilter
 */
@SuppressWarnings("serial")
public class MultipartException extends NestedRuntimeException {

	/**
	 * Constructor for MultipartException.
	 * @param msg the detail message
	 */
	public MultipartException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MultipartException.
	 * @param msg the detail message
	 * @param cause the root cause from the multipart parsing API in use
	 */
	public MultipartException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
