/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.datetime.joda;

import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} that creates a Joda {@link DateTimeFormatter}. Formatters will be
 * created using the defined {@link #setPattern(String) pattern}, {@link #setIso(ISO) ISO},
 * or {@link #setStyle(String) style} (considered in that order).
 *
 * @author Phillip Webb
 * @see #getDateTimeFormatter()
 * @see #getDateTimeFormatter(DateTimeFormatter)
 * @since 3.2
 */
public class DateTimeFormatterFactory implements FactoryBean<DateTimeFormatter> {

	private ISO iso;

	private String style;

	private String pattern;

	private TimeZone timeZone;


	/**
	 * Create a new {@code DateTimeFormatterFactory} instance.
	 */
	public DateTimeFormatterFactory() {
	}

	/**
	 * Create a new {@code DateTimeFormatterFactory} instance.
	 * @param pattern the pattern to use to format date values
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}


	public boolean isSingleton() {
		return true;
	}

	public Class<?> getObjectType() {
		return DateTimeFormatter.class;
	}

	public DateTimeFormatter getObject() throws Exception {
		return getDateTimeFormatter();
	}

	/**
	 * Get a new {@code DateTimeFormatter} using this factory. If no specific
	 * {@link #setStyle(String) style}, {@link #setIso(ISO) ISO}, or
	 * {@link #setPattern(String) pattern} have been defined the
	 * {@link DateTimeFormat#mediumDateTime() medium date time format} will be used.
	 * @return a new date time formatter
	 * @see #getObject()
	 * @see #getDateTimeFormatter(DateTimeFormatter)
	 */
	public DateTimeFormatter getDateTimeFormatter() {
		return getDateTimeFormatter(DateTimeFormat.mediumDateTime());
	}

	/**
	 * Get a new {@code DateTimeFormatter} using this factory. If no specific
	 * {@link #setStyle(String) style}, {@link #setIso(ISO) ISO}, or
	 * {@link #setPattern(String) pattern} have been defined the supplied
	 * {@code fallbackFormatter} will be used.
	 * @param fallbackFormatter the fall-back formatter to use when no specific factory
	 *        properties have been set (can be {@code null}).
	 * @return a new date time formatter
	 */
	public DateTimeFormatter getDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = createDateTimeFormatter();
		if(dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter.withZone(DateTimeZone.forTimeZone(this.timeZone));
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

	private DateTimeFormatter createDateTimeFormatter() {
		if (StringUtils.hasLength(pattern)) {
			return DateTimeFormat.forPattern(pattern);
		}
		if (iso != null && iso != ISO.NONE) {
			if (iso == ISO.DATE) {
				return ISODateTimeFormat.date();
			}
			if (iso == ISO.TIME) {
				return ISODateTimeFormat.time();
			}
			return ISODateTimeFormat.dateTime();
		}
		if (StringUtils.hasLength(style)) {
			return DateTimeFormat.forStyle(style);
		}
		return null;
	}


	/**
	 * Set the {@code TimeZone} to normalize the date values into, if any.
	 * @param timeZone the time zone
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Set the two characters to use to format date values. The first character is used for
	 * the date style; the second is for the time style. Supported characters are:
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * <p>This method mimics the styles supported by Joda Time.
	 * @param style two characters from the set {"S", "M", "L", "F", "-"}
	 */
	public void setStyle(String style) {
		this.style = style;
	}

	/**
	 * Set the ISO format used to format date values.
	 * @param iso the ISO format
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * Set the pattern to use to format date values.
	 * @param pattern the format pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

}
