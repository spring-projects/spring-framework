/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.MonthDay;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link MonthDay},
 * following JSR-310's parsing rules for a MonthDay.
 *
 * @author Juergen Hoeller
 * @since 4.2.4
 * @see MonthDay#parse
 */
class MonthDayFormatter implements Formatter<MonthDay> {

	@Override
	public MonthDay parse(String text, Locale locale) throws ParseException {
		return MonthDay.parse(text);
	}

	@Override
	public String print(MonthDay object, Locale locale) {
		return object.toString();
	}

}
