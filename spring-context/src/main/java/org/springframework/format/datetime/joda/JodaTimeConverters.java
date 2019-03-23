/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;

/**
 * Installs lower-level type converters required to integrate
 * Joda-Time support into Spring's field formatting system.
 *
 * <p>Note: {@link JodaTimeFormatterRegistrar} installs these converters
 * and relies on several of them for its formatters. Some additional
 * converters are just being registered for custom conversion scenarios.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 3.0
 */
final class JodaTimeConverters {

	/**
	 * Install the converters into the converter registry.
	 * @param registry the converter registry
	 */
	@SuppressWarnings("deprecation")
	public static void registerConverters(ConverterRegistry registry) {
		DateFormatterRegistrar.addDateConverters(registry);

		registry.addConverter(new DateTimeToLocalDateConverter());
		registry.addConverter(new DateTimeToLocalTimeConverter());
		registry.addConverter(new DateTimeToLocalDateTimeConverter());
		registry.addConverter(new DateTimeToDateMidnightConverter());
		registry.addConverter(new DateTimeToMutableDateTimeConverter());
		registry.addConverter(new DateTimeToInstantConverter());
		registry.addConverter(new DateTimeToDateConverter());
		registry.addConverter(new DateTimeToCalendarConverter());
		registry.addConverter(new DateTimeToLongConverter());
		registry.addConverter(new DateToReadableInstantConverter());
		registry.addConverter(new CalendarToReadableInstantConverter());
		registry.addConverter(new LongToReadableInstantConverter());
		registry.addConverter(new LocalDateTimeToLocalDateConverter());
		registry.addConverter(new LocalDateTimeToLocalTimeConverter());
	}


	private static class DateTimeToLocalDateConverter implements Converter<DateTime, LocalDate> {

		@Override
		public LocalDate convert(DateTime source) {
			return source.toLocalDate();
		}
	}


	private static class DateTimeToLocalTimeConverter implements Converter<DateTime, LocalTime> {

		@Override
		public LocalTime convert(DateTime source) {
			return source.toLocalTime();
		}
	}


	private static class DateTimeToLocalDateTimeConverter implements Converter<DateTime, LocalDateTime> {

		@Override
		public LocalDateTime convert(DateTime source) {
			return source.toLocalDateTime();
		}
	}


	@Deprecated
	private static class DateTimeToDateMidnightConverter implements Converter<DateTime, org.joda.time.DateMidnight> {

		@Override
		public org.joda.time.DateMidnight convert(DateTime source) {
			return source.toDateMidnight();
		}
	}


	private static class DateTimeToMutableDateTimeConverter implements Converter<DateTime, MutableDateTime> {

		@Override
		public MutableDateTime convert(DateTime source) {
			return source.toMutableDateTime();
		}
	}


	private static class DateTimeToInstantConverter implements Converter<DateTime, Instant> {

		@Override
		public Instant convert(DateTime source) {
			return source.toInstant();
		}
	}


	private static class DateTimeToDateConverter implements Converter<DateTime, Date> {

		@Override
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}


	private static class DateTimeToCalendarConverter implements Converter<DateTime, Calendar> {

		@Override
		public Calendar convert(DateTime source) {
			return source.toGregorianCalendar();
		}
	}


	private static class DateTimeToLongConverter implements Converter<DateTime, Long> {

		@Override
		public Long convert(DateTime source) {
			return source.getMillis();
		}
	}


	/**
	 * Used when printing a {@code java.util.Date} field with a ReadableInstantPrinter.
	 * @see MillisecondInstantPrinter
	 * @see JodaDateTimeFormatAnnotationFormatterFactory
	 */
	private static class DateToReadableInstantConverter implements Converter<Date, ReadableInstant> {

		@Override
		public ReadableInstant convert(Date source) {
			return new DateTime(source);
		}
	}


	/**
	 * Used when printing a {@code java.util.Calendar} field with a ReadableInstantPrinter.
	 * @see MillisecondInstantPrinter
	 * @see JodaDateTimeFormatAnnotationFormatterFactory
	 */
	private static class CalendarToReadableInstantConverter implements Converter<Calendar, ReadableInstant> {

		@Override
		public ReadableInstant convert(Calendar source) {
			return new DateTime(source);
		}
	}


	/**
	 * Used when printing a Long field with a ReadableInstantPrinter.
	 * @see MillisecondInstantPrinter
	 * @see JodaDateTimeFormatAnnotationFormatterFactory
	 */
	private static class LongToReadableInstantConverter implements Converter<Long, ReadableInstant> {

		@Override
		public ReadableInstant convert(Long source) {
			return new DateTime(source.longValue());
		}
	}


	private static class LocalDateTimeToLocalDateConverter implements Converter<LocalDateTime, LocalDate> {

		@Override
		public LocalDate convert(LocalDateTime source) {
			return source.toLocalDate();
		}
	}


	private static class LocalDateTimeToLocalTimeConverter implements Converter<LocalDateTime, LocalTime> {

		@Override
		public LocalTime convert(LocalDateTime source) {
			return source.toLocalTime();
		}
	}

}
