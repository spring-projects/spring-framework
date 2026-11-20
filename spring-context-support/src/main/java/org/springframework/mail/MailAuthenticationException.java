/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

/**
 * Exception thrown on failed authentication.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MailAuthenticationException extends MailException {

	/**
	 * Constructor for MailAuthenticationException.
	 * @param msg message
	 */
	public MailAuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailAuthenticationException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailAuthenticationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor for MailAuthenticationException.
	 * @param cause the root cause from the mail API in use
	 */
	public MailAuthenticationException(Throwable cause) {
		super("Authentication failed", cause);
	}

}
