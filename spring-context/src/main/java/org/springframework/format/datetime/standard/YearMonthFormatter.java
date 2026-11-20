/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.YearMonth;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link YearMonth},
 * following JSR-310's parsing rules for a YearMonth.
 *
 * @author Juergen Hoeller
 * @since 4.2.4
 * @see YearMonth#parse
 */
class YearMonthFormatter implements Formatter<YearMonth> {

	@Override
	public YearMonth parse(String text, Locale locale) throws ParseException {
		return YearMonth.parse(text);
	}

	@Override
	public String print(YearMonth object, Locale locale) {
		return object.toString();
	}

}
