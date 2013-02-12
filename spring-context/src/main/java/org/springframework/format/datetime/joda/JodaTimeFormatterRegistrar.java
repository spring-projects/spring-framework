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
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Configures Joda Time's formatting system for use with Spring.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see FormatterRegistrar#registerFormatters
 * @see org.springframework.format.datetime.DateFormatterRegistrar
 * @see DateTimeFormatterFactoryBean
 */
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

	/**
	 * User defined formatters.
	 */
	private Map<Type, DateTimeFormatter> formatters = new HashMap<Type, DateTimeFormatter>();

	/**
	 * Factories used when specific formatters have not been specified.
	 */
	private Map<Type, DateTimeFormatterFactory> factories;


	public JodaTimeFormatterRegistrar() {
		this.factories = new HashMap<Type, DateTimeFormatterFactory>();
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * Set the default format style of Joda {@link LocalDate} objects.
	 * Default is {@link DateTimeFormat#shortDate()}.
	 */
	public void setDateStyle(String dateStyle) {
		this.factories.get(Type.DATE).setStyle(dateStyle+"-");
	}

	/**
	 * Set the default format style of Joda {@link LocalTime} objects.
	 * Default is {@link DateTimeFormat#shortTime()}.
	 */
	public void setTimeStyle(String timeStyle) {
		this.factories.get(Type.TIME).setStyle("-"+timeStyle);
	}

	/**
	 * Set the default format style of Joda {@link LocalDateTime} and {@link DateTime} objects,
	 * as well as JDK {@link Date} and {@link Calendar} objects.
	 * Default is {@link DateTimeFormat#shortDateTime()}.
	 */
	public void setDateTimeStyle(String dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
	}

	/**
	 * Set whether standard ISO formatting should be applied to all Date/Time types.
	 * Default is false (no).
	 * If set to true, the dateStyle, timeStyle, and dateTimeStyle properties are ignored.
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : null);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : null);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : null);
	}

	/**
	 * Set the formatter that will be used for objects representing date values.
	 * <p>This formatter will be used for the {@link LocalDate} type. When specified
	 * the {@link #setDateStyle(String) dateStyle} and
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} properties will be ignored.
	 * @param formatter the formatter to use
	 * @see #setTimeFormatter(DateTimeFormatter)
	 * @see #setDateTimeFormatter(DateTimeFormatter)
	 * @since 3.2
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * Set the formatter that will be used for objects representing time values.
	 * <p>This formatter will be used for the {@link LocalTime} type. When specified
	 * the {@link #setTimeStyle(String) timeStyle} and
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} properties will be ignored.
	 * @param formatter the formatter to use
	 * @see #setDateFormatter(DateTimeFormatter)
	 * @see #setDateTimeFormatter(DateTimeFormatter)
	 * @since 3.2
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * Set the formatter that will be used for objects representing date and time values.
	 * <p>This formatter will be used for {@link LocalDateTime}, {@link ReadableInstant},
	 * {@link Date} and {@link Calendar} types. When specified
	 * the {@link #setDateTimeStyle(String) dateTimeStyle} and
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} properties will be ignored.
	 * @param formatter the formatter to use
	 * @see #setDateFormatter(DateTimeFormatter)
	 * @see #setTimeFormatter(DateTimeFormatter)
	 * @since 3.2
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}

	public void registerFormatters(FormatterRegistry registry) {
		JodaTimeConverters.registerConverters(registry);

		DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
		DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
		DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateFormatter),
				new DateTimeParser(dateFormatter),
				LocalDate.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(timeFormatter),
				new DateTimeParser(timeFormatter),
				LocalTime.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateTimeFormatter),
				new DateTimeParser(dateTimeFormatter),
				LocalDateTime.class);

		addFormatterForFields(registry,
				new ReadableInstantPrinter(dateTimeFormatter),
				new DateTimeParser(dateTimeFormatter),
				ReadableInstant.class, Date.class, Calendar.class);

		registry.addFormatterForFieldAnnotation(
				new JodaDateTimeFormatAnnotationFormatterFactory());
	}

	private DateTimeFormatter getFormatter(Type type) {
		DateTimeFormatter formatter = this.formatters.get(type);
		if(formatter != null) {
			return formatter;
		}
		DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
		return this.factories.get(type).createDateTimeFormatter(fallbackFormatter );
	}

	private DateTimeFormatter getFallbackFormatter(Type type) {
		switch (type) {
			case DATE: return DateTimeFormat.shortDate();
			case TIME: return DateTimeFormat.shortTime();
			default: return DateTimeFormat.shortDateTime();
		}
	}

	private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer,
			Parser<?> parser, Class<?>... fieldTypes) {
		for (Class<?> fieldType : fieldTypes) {
			registry.addFormatterForFieldType(fieldType, printer, parser);
		}
	}

	private static enum Type {DATE, TIME, DATE_TIME}
}
