/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import java.util.Locale;

/**
 * Exception thrown when a message can't be resolved.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class NoSuchMessageException extends RuntimeException {

	/**
	 * Create a new exception.
	 * @param code the code that could not be resolved for given locale
	 * @param locale the locale that was used to search for the code within
	 */
	public NoSuchMessageException(String code, Locale locale) {
		super("No message found under code '" + code + "' for locale '" + locale + "'.");
	}

	/**
	 * Create a new exception.
	 * @param code the code that could not be resolved for given locale
	 */
	public NoSuchMessageException(String code) {
		super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
	}

}

