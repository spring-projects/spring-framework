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

package org.springframework.format.datetime.joda;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * Installs lower-level type converters required to integrate Joda Time support into Spring's field formatting system.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class JodaTimeConverters {

	/**
	 * Install the converters into the converter registry.
	 * @param registry the converter registry
	 */
	public static void registerConverters(ConverterRegistry registry) {
		registry.addConverter(new DateTimeToLocalDateConverter());
		registry.addConverter(new DateTimeToLocalTimeConverter());
		registry.addConverter(new DateTimeToLocalDateTimeConverter());
		registry.addConverter(new DateTimeToDateMidnightConverter());
		registry.addConverter(new DateTimeToInstantConverter());
		registry.addConverter(new DateTimeToMutableDateTimeConverter());
		registry.addConverter(new DateTimeToDateConverter());
		registry.addConverter(new DateTimeToCalendarConverter());
		registry.addConverter(new DateTimeToLongConverter());
		registry.addConverter(new DateToLongConverter());
		registry.addConverter(new CalendarToReadableInstantConverter());
	}


	/**
	 * Used when binding a parsed DateTime to a LocalDate field.
	 * @see DateTimeParser 
	 **/
	private static class DateTimeToLocalDateConverter implements Converter<DateTime, LocalDate> {
		public LocalDate convert(DateTime source) {
			return source.toLocalDate();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a LocalTime field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToLocalTimeConverter implements Converter<DateTime, LocalTime> {
		public LocalTime convert(DateTime source) {
			return source.toLocalTime();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a LocalDateTime field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToLocalDateTimeConverter implements Converter<DateTime, LocalDateTime> {
		public LocalDateTime convert(DateTime source) {
			return source.toLocalDateTime();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a DateMidnight field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToDateMidnightConverter implements Converter<DateTime, DateMidnight> {
		public DateMidnight convert(DateTime source) {
			return source.toDateMidnight();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to an Instant field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToInstantConverter implements Converter<DateTime, Instant> {
		public Instant convert(DateTime source) {
			return source.toInstant();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a MutableDateTime field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToMutableDateTimeConverter implements Converter<DateTime, MutableDateTime> {
		public MutableDateTime convert(DateTime source) {
			return source.toMutableDateTime();
		}
	}
	
	/** 
	 * Used when binding a parsed DateTime to a java.util.Date field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToDateConverter implements Converter<DateTime, Date> {
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a java.util.Calendar field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToCalendarConverter implements Converter<DateTime, Calendar> {
		public Calendar convert(DateTime source) {
			return source.toGregorianCalendar();
		}
	}

	/** 
	 * Used when binding a parsed DateTime to a java.lang.Long field. 
	 * @see DateTimeParser 
	 */
	private static class DateTimeToLongConverter implements Converter<DateTime, Long> {
		public Long convert(DateTime source) {
			return source.getMillis();
		}
	}

	/** 
	 * Used when printing a java.util.Date field with a MillisecondInstantPrinter.
	 * @see MillisecondInstantPrinter
	 * @see JodaDateTimeFormatAnnotationFormatterFactory
	 */	
	private static class DateToLongConverter implements Converter<Date, Long> {
		public Long convert(Date source) {
			return source.getTime();
		}
	}

	/** 
	 * Used when printing a java.util.Calendar field with a ReadableInstantPrinter.
	 * @see MillisecondInstantPrinter
	 * @see JodaDateTimeFormatAnnotationFormatterFactory
	 */	
	private static class CalendarToReadableInstantConverter implements Converter<Calendar, ReadableInstant> {
		public ReadableInstant convert(Calendar source) {
			return new DateTime(source);
		}
	}

}
