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

package org.springframework.format.datetime;

import java.util.Calendar;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.util.Assert;

/**
 * Configures Date formatting for use with Spring.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addDateConverters(ConverterRegistry)} utility method for ad hoc use
 * against any {@code ConverterRegistry} instance.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar
 * @see FormatterRegistrar#registerFormatters
 */
public class DateFormatterRegistrar implements FormatterRegistrar {


	private DateFormatter dateFormatter = new DateFormatter();


	public void registerFormatters(FormatterRegistry registry) {
		addDateConverters(registry);
		registry.addFormatter(dateFormatter);
		registry.addFormatterForFieldType(Calendar.class, dateFormatter);
		registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());
	}

	/**
	 * Set the date formatter to register. If not specified the default {@link DateFormatter}
	 * will be used. This method can be used if additional formatter configuration is
	 * required.
	 * @param dateFormatter the date formatter
	 */
	public void setFormatter(DateFormatter dateFormatter) {
		Assert.notNull(dateFormatter,"DateFormatter must not be null");
		this.dateFormatter = dateFormatter;
	}

	/**
	 * Add date converters to the specified registry.
	 * @param converterRegistry the registry of converters to add to
	 */
	public static void addDateConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverter(new DateToLongConverter());
		converterRegistry.addConverter(new DateToCalendarConverter());
		converterRegistry.addConverter(new CalendarToDateConverter());
		converterRegistry.addConverter(new CalendarToLongConverter());
		converterRegistry.addConverter(new LongToDateConverter());
		converterRegistry.addConverter(new LongToCalendarConverter());
	}


	private static class DateToLongConverter implements Converter<Date, Long> {
		public Long convert(Date source) {
			return source.getTime();
		}
	}


	private static class DateToCalendarConverter implements Converter<Date, Calendar> {
		public Calendar convert(Date source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(source);
			return calendar;
		}
	}


	private static class CalendarToDateConverter implements Converter<Calendar, Date> {
		public Date convert(Calendar source) {
			return source.getTime();
		}
	}


	private static class CalendarToLongConverter implements Converter<Calendar, Long> {
		public Long convert(Calendar source) {
			return source.getTime().getTime();
		}
	}


	private static class LongToDateConverter implements Converter<Long, Date> {
		public Date convert(Long source) {
			return new Date(source);
		}
	}


	private static class LongToCalendarConverter implements Converter<Long, Calendar> {

		private DateToCalendarConverter dateToCalendarConverter = new DateToCalendarConverter();

		public Calendar convert(Long source) {
			return dateToCalendarConverter.convert(new Date(source));
		}
	}
}
