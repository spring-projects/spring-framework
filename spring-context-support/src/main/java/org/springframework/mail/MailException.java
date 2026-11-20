/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for all mail exceptions.
 *
 * @author Dmitriy Kopylenko
 */
@SuppressWarnings("serial")
public abstract class MailException extends NestedRuntimeException {

	/**
	 * Constructor for MailException.
	 * @param msg the detail message
	 */
	public MailException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
