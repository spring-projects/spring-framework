/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.springframework.format.Printer;

/**
 * {@link Printer} implementation for a JSR-310 {@link java.time.temporal.TemporalAccessor},
 * using a {@link java.time.format.DateTimeFormatter}) (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see DateTimeContextHolder#getFormatter
 * @see java.time.format.DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
 */
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {

	private final DateTimeFormatter formatter;


	/**
	 * Create a new TemporalAccessorPrinter.
	 * @param formatter the base DateTimeFormatter instance
	 */
	public TemporalAccessorPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(TemporalAccessor partial, Locale locale) {
		return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
	}

}
