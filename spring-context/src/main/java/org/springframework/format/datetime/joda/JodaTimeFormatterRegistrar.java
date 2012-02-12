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

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

/**
 * Configures Joda Time's Formatting system for use with Spring.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see FormatterRegistrar#registerFormatters
 */
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

	private String dateStyle;

	private String timeStyle;

	private String dateTimeStyle;

	private boolean useIsoFormat;

	/**
	 * Set the default format style of Joda {@link LocalDate} objects.
	 * Default is {@link DateTimeFormat#shortDate()}.
	 */
	public void setDateStyle(String dateStyle) {
		this.dateStyle = dateStyle;
	}

	/**
	 * Set the default format style of Joda {@link LocalTime} objects.
	 * Default is {@link DateTimeFormat#shortTime()}.
	 */
	public void setTimeStyle(String timeStyle) {
		this.timeStyle = timeStyle;
	}

	/**
	 * Set the default format style of Joda {@link LocalDateTime} and {@link DateTime} objects,
	 * as well as JDK {@link Date} and {@link Calendar} objects.
	 * Default is {@link DateTimeFormat#shortDateTime()}.
	 */
	public void setDateTimeStyle(String dateTimeStyle) {
		this.dateTimeStyle = dateTimeStyle;
	}

	/**
	 * Set whether standard ISO formatting should be applied to all Date/Time types.
	 * Default is false (no).
	 * If set to true, the dateStyle, timeStyle, and dateTimeStyle properties are ignored.
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.useIsoFormat = useIsoFormat;
	}

	public void registerFormatters(FormatterRegistry registry) {
		JodaTimeConverters.registerConverters(registry);

		DateTimeFormatter jodaDateFormatter = getJodaDateFormatter();
		registry.addFormatterForFieldType(LocalDate.class, new ReadablePartialPrinter(jodaDateFormatter),
				new DateTimeParser(jodaDateFormatter));

		DateTimeFormatter jodaTimeFormatter = getJodaTimeFormatter();
		registry.addFormatterForFieldType(LocalTime.class, new ReadablePartialPrinter(jodaTimeFormatter),
				new DateTimeParser(jodaTimeFormatter));

		DateTimeFormatter jodaDateTimeFormatter = getJodaDateTimeFormatter();
		Parser<DateTime> dateTimeParser = new DateTimeParser(jodaDateTimeFormatter);
		registry.addFormatterForFieldType(LocalDateTime.class, new ReadablePartialPrinter(jodaDateTimeFormatter),
				dateTimeParser);

		Printer<ReadableInstant> readableInstantPrinter = new ReadableInstantPrinter(jodaDateTimeFormatter);
		registry.addFormatterForFieldType(ReadableInstant.class, readableInstantPrinter, dateTimeParser);

		registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
	}

	// internal helpers

	private DateTimeFormatter getJodaDateFormatter() {
		if (this.useIsoFormat) {
			return ISODateTimeFormat.date();
		}
		if (this.dateStyle != null) {
			return DateTimeFormat.forStyle(this.dateStyle + "-");
		} else {
			return DateTimeFormat.shortDate();
		}
	}

	private DateTimeFormatter getJodaTimeFormatter() {
		if (this.useIsoFormat) {
			return ISODateTimeFormat.time();
		}
		if (this.timeStyle != null) {
			return DateTimeFormat.forStyle("-" + this.timeStyle);
		} else {
			return DateTimeFormat.shortTime();
		}
	}

	private DateTimeFormatter getJodaDateTimeFormatter() {
		if (this.useIsoFormat) {
			return ISODateTimeFormat.dateTime();
		}
		if (this.dateTimeStyle != null) {
			return DateTimeFormat.forStyle(this.dateTimeStyle);
		} else {
			return DateTimeFormat.shortDateTime();
		}
	}

}
