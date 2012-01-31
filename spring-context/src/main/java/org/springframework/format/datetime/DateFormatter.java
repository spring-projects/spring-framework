/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.format.datetime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.format.Formatter;

/**
 * A formatter for {@link java.util.Date} types.
 * Allows the configuration of an explicit date pattern and locale.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see SimpleDateFormat 
 */
public class DateFormatter implements Formatter<Date> {

	private String pattern;

	private int style = DateFormat.DEFAULT;

	private TimeZone timeZone;

	private boolean lenient = false;


	/**
	 * Create a new default DateFormatter.
	 */
	public DateFormatter() {
	}

	/**
	 * Create a new DateFormatter for the given date pattern.
	 */
	public DateFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * Set the pattern to use to format date values.
	 * <p>If not specified, DateFormat's default style will be used.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Set the style to use to format date values.
	 * <p>If not specified, DateFormat's default style will be used.
	 * @see DateFormat#DEFAULT
	 * @see DateFormat#SHORT
	 * @see DateFormat#MEDIUM
	 * @see DateFormat#LONG
	 * @see DateFormat#FULL
	 */
	public void setStyle(int style) {
		this.style = style;
	}

	/**
	 * Set the TimeZone to normalize the date values into, if any.
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Specify whether or not parsing is to be lenient. Default is false.
	 * <p>With lenient parsing, the parser may allow inputs that do not precisely match the format.
	 * With strict parsing, inputs must match the format exactly.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	public String print(Date date, Locale locale) {
		return getDateFormat(locale).format(date);
	}

	public Date parse(String text, Locale locale) throws ParseException {
		return getDateFormat(locale).parse(text);
	}


	protected DateFormat getDateFormat(Locale locale) {
		DateFormat dateFormat;
		if (this.pattern != null) {
			dateFormat = new SimpleDateFormat(this.pattern, locale);
		}
		else {
			dateFormat = DateFormat.getDateInstance(this.style, locale);
		}
		if (this.timeZone != null) {
			dateFormat.setTimeZone(this.timeZone);
		}
		dateFormat.setLenient(this.lenient);
		return dateFormat;
	}

}
