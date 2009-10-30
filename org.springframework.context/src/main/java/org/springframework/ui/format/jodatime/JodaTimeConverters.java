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
package org.springframework.ui.format.jodatime;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * Installs lower-level type converters required to integrate Joda Time support into Spring's formatting and property binding systems.
 * @author Keith Donald 
 */
final class JodaTimeConverters {

	private JodaTimeConverters() {
		
	}
	
	/**
	 * Installs the converters into the converter registry.
	 * @param registry the converter registry
	 */
	public static void registerConverters(ConverterRegistry registry) {
		registry.addConverter(new DateTimeToLocalDateConverter());
		registry.addConverter(new DateTimeToLocalTimeConverter());
		registry.addConverter(new DateTimeToLocalDateTimeConverter());
		registry.addConverter(new DateTimeToDateMidnightConverter());
		registry.addConverter(new DateTimeToDateConverter());
		registry.addConverter(new DateTimeToCalendarConverter());
		registry.addConverter(new DateToLongConverter());
		registry.addConverter(new CalendarToDateTimeConverter());
	}

	// internal helpers
	
	// used when binding a parsed DateTime to a LocalDate property
	private static class DateTimeToLocalDateConverter implements Converter<DateTime, LocalDate> {
		public LocalDate convert(DateTime source) {
			return source.toLocalDate();
		}
	}

	// used when binding a parsed DateTime to a LocalTime property
	private static class DateTimeToLocalTimeConverter implements Converter<DateTime, LocalTime> {
		public LocalTime convert(DateTime source) {
			return source.toLocalTime();
		}
	}

	// used when binding a parsed DateTime to a LocalDateTime property
	private static class DateTimeToLocalDateTimeConverter implements Converter<DateTime, LocalDateTime> {
		public LocalDateTime convert(DateTime source) {
			return source.toLocalDateTime();
		}
	}

	// used when binding a parsed DateTime to a DateMidnight property
	private static class DateTimeToDateMidnightConverter implements Converter<DateTime, DateMidnight> {
		public DateMidnight convert(DateTime source) {
			return source.toDateMidnight();
		}
	}

	// used when binding a parsed DateTime to a java.util.Date property
	private static class DateTimeToDateConverter implements Converter<DateTime, Date> {
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}

	// used when binding a parsed DateTime to a java.util.Calendar property
	private static class DateTimeToCalendarConverter implements Converter<DateTime, Calendar> {
		public Calendar convert(DateTime source) {
			return source.toGregorianCalendar();
		}
	}

	// used when formatting a java.util.Date property with a MillisecondInstantPrinter
	private static class DateToLongConverter implements Converter<Date, Long> {
		public Long convert(Date source) {
			return source.getTime();
		}
	}

	// used when formatting a java.util.Calendar property with a ReadableInstantPrinter
	private static class CalendarToDateTimeConverter implements Converter<Calendar, ReadableInstant> {
		public ReadableInstant convert(Calendar source) {
			return new DateTime(source);
		}
	}

}
