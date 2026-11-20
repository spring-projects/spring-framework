/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail;

/**
 * Exception to be thrown by user code if a mail cannot be prepared properly,
 * for example when a FreeMarker template cannot be rendered for the mail text.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.ui.freemarker.FreeMarkerTemplateUtils#processTemplateIntoString
 */
@SuppressWarnings("serial")
public class MailPreparationException extends MailException {

	/**
	 * Constructor for MailPreparationException.
	 * @param msg the detail message
	 */
	public MailPreparationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MailPreparationException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailPreparationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailPreparationException(Throwable cause) {
		super("Could not prepare mail", cause);
	}

}
