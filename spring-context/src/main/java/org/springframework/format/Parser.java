/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format;

import java.text.ParseException;
import java.util.Locale;

/**
 * Parses text strings to produce instances of T.
 *
 * @author Keith Donald
 * @since 3.0
 * @param <T> the type of object this Parser produces
 */
@FunctionalInterface
public interface Parser<T> {

	/**
	 * Parse a text String to produce a T.
	 * @param text the text string
	 * @param locale the current user locale
	 * @return an instance of T
	 * @throws ParseException when a parse exception occurs in a java.text parsing library
	 * @throws IllegalArgumentException when a parse exception occurs
	 */
	T parse(String text, Locale locale) throws ParseException;

}
