/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.format;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A formatter for {@link Date} types.
 * Allows the configuration of an explicit date pattern and locale.
 * @see SimpleDateFormat
 * @author Keith Donald
 */
public class DateFormatter implements Formatter<Date> {

	private static Log logger = LogFactory.getLog(DateFormatter.class);

	/**
	 * The default date pattern.
	 */
	private static final String DEFAULT_PATTERN = "yyyy-MM-dd";

	private String pattern;

	/**
	 * Sets the pattern to use to format date values.
	 * If not specified, the default pattern 'yyyy-MM-dd' is used. 
	 * @param pattern the date formatting pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String format(Date date, Locale locale) {
		if (date == null) {
			return "";
		}
		return getDateFormat(locale).format(date);
	}

	public Date parse(String formatted, Locale locale) throws ParseException {
		if (formatted.length() == 0) {
			return null;
		}
		return getDateFormat(locale).parse(formatted);
	}

	// subclassing hookings

	protected DateFormat getDateFormat(Locale locale) {
		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		format.setLenient(false);
		if (format instanceof SimpleDateFormat) {
			String pattern = determinePattern(this.pattern);
			((SimpleDateFormat) format).applyPattern(pattern);
		} else {
			logger.warn("Unable to apply format pattern '" + pattern
					+ "'; Returned DateFormat is not a SimpleDateFormat");
		}
		return format;
	}

	// internal helpers

	private String determinePattern(String pattern) {
		return pattern != null ? pattern : DEFAULT_PATTERN;
	}

}