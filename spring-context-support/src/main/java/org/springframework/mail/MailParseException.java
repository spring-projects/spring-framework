/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

/**
 * Exception thrown if illegal message properties are encountered.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MailParseException extends MailException {

	/**
	 * Constructor for MailParseException.
	 * @param msg the detail message
	 */
	public MailParseException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailParseException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailParseException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor for MailParseException.
	 * @param cause the root cause from the mail API in use
	 */
	public MailParseException(Throwable cause) {
		super("Could not parse mail", cause);
	}

}
