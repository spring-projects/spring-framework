/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Year;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Year},
 * following JSR-310's parsing rules for a Year.
 *
 * @author Juergen Hoeller
 * @since 5.0.4
 * @see Year#parse
 */
class YearFormatter implements Formatter<Year> {

	@Override
	public Year parse(String text, Locale locale) throws ParseException {
		return Year.parse(text);
	}

	@Override
	public String print(Year object, Locale locale) {
		return object.toString();
	}

}
