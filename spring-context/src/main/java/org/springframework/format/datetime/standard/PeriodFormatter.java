/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Period;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Period},
 * following JSR-310's parsing rules for a Period.
 *
 * @author Juergen Hoeller
 * @since 4.2.4
 * @see Period#parse
 */
class PeriodFormatter implements Formatter<Period> {

	@Override
	public Period parse(String text, Locale locale) throws ParseException {
		return Period.parse(text);
	}

	@Override
	public String print(Period object, Locale locale) {
		return object.toString();
	}

}
